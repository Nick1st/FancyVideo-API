package nick1st.fancyvideo;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.realmsclient.gui.screens.RealmsGenericErrorScreen;
import com.sun.jna.NativeLibrary;
import net.minecraft.client.gui.screen.OptionsScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.WorldSelectionScreen;
import net.minecraftforge.client.event.GuiScreenEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.InterModComms;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.InterModEnqueueEvent;
import net.minecraftforge.fml.event.lifecycle.InterModProcessEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import nick1st.fancyvideo.api.MediaPlayer;
import nick1st.fancyvideo.api.MediaPlayers;
import org.apache.commons.compress.utils.IOUtils;
import org.apache.commons.lang3.SystemUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import uk.co.caprica.vlcj.binding.RuntimeUtil;
import uk.co.caprica.vlcj.factory.NativeLibraryMappingException;
import uk.co.caprica.vlcj.factory.discovery.NativeDiscovery;
import uk.co.caprica.vlcj.factory.discovery.strategy.NativeDiscoveryStrategy;
import uk.co.caprica.vlcj.player.component.CallbackMediaPlayerComponent;
import uk.co.caprica.vlcj.player.embedded.videosurface.callback.BufferFormat;
import uk.co.caprica.vlcj.player.embedded.videosurface.callback.BufferFormatCallbackAdapter;
import uk.co.caprica.vlcj.player.embedded.videosurface.callback.format.RV32BufferFormat;
import uk.co.caprica.vlcj.support.Info;
import uk.co.caprica.vlcj.support.version.LibVlcVersion;
import uk.co.caprica.vlcj.support.version.Version;

import java.awt.image.BufferedImage;
import java.io.*;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Semaphore;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static uk.co.caprica.vlcj.binding.LibVlc.libvlc_get_version;

// The value here should match an entry in the META-INF/mods.toml file
@Mod("fancyvideo")
public class FancyVideo {

    private final CallbackMediaPlayerComponent mediaPlayerComponent;
    private final VideoRenderCallback defaultRenderCallback = new VideoRenderCallback();

    // Synced objects
    static Semaphore semaphore = new Semaphore(1, true);
    static int[] frame = new int[0];
    static int width;

    private boolean init;

    private final NativeDiscovery discovery = new NativeDiscovery();

    // Directly reference a log4j logger.
    private static final Logger LOGGER = LogManager.getLogger("fancyvideo");

    // Temp Objects only for testing
    int frameNumb = 0;

    public FancyVideo() {
        // Check or create VLC availability
        makeVlcAvailable();

        // Create our media callback
        DefaultBufferFormatCallback defaultBufferFormatCallback = new DefaultBufferFormatCallback();
        mediaPlayerComponent = new CallbackMediaPlayerComponent(null, null, null, true, null, defaultRenderCallback, defaultBufferFormatCallback, null);

        // Register the enqueueIMC and processIMC method for modloading
        FMLJavaModLoadingContext.get().getModEventBus().addListener(this::enqueueIMC);
        FMLJavaModLoadingContext.get().getModEventBus().addListener(this::processIMC);

        // Register ourselves for server and other game events we are interested in
        MinecraftForge.EVENT_BUS.register(this);
    }

    /**
     * Default implementation of a buffer format callback that returns a buffer format suitable for rendering into a
     * {@link BufferedImage}.
     */
    private class DefaultBufferFormatCallback extends BufferFormatCallbackAdapter {

        @Override
        public BufferFormat getBufferFormat(int sourceWidth, int sourceHeight) {
            newVideoBuffer(sourceWidth, sourceHeight);
            return new RV32BufferFormat(sourceWidth, sourceHeight);
        }

        private void newVideoBuffer(int width, int height) {
            BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
            defaultRenderCallback.setImageBuffer(image);
        }
    }

    private void enqueueIMC(final InterModEnqueueEvent event) {
        InterModComms.sendTo("konkrete", "videoPluginInit", () -> {
            LOGGER.info("Hello world from vlcj plugin");
            return "Hello from vlcj";
        });
    }
    private void processIMC(final InterModProcessEvent event) {
        LOGGER.info("Got IMC {}", event.getIMCStream().
                map(m -> m.getMessageSupplier().get()).
                collect(Collectors.toList()));
    }

    public boolean makeVlcAvailable() {
        LOGGER.info("Trying to get VLC context");
        LOGGER.debug(Info.getInstance());

        // resolve architecture
        Map<String, String> archMap = new HashMap<>();
        archMap.put("x86", "32");
        archMap.put("i386", "32");
        archMap.put("i486", "32");
        archMap.put("i586", "32");
        archMap.put("i686", "32");
        archMap.put("x86_64", "64");
        archMap.put("amd64", "64");
        archMap.put("arm64", "mac");
        archMap.put("powerpc", "ppc");
        String arch = archMap.get(SystemUtils.OS_ARCH);
        System.getProperty("os.arch");

        if (arch == null) {
            throw new IllegalArgumentException("Unknown architecture " + SystemUtils.OS_ARCH);
        } else if (arch.equals("ppc")) {
            throw new IllegalArgumentException("Unsupported architecture " + SystemUtils.OS_ARCH);
        }

        // Get correct os and arch version and check for existence
        if (SystemUtils.IS_OS_WINDOWS) {
            if (!new File("libvlc.dll").isFile() && !new File("libvlccore.dll").isFile()) {
                LOGGER.info("Couldn't load vlc binaries, unpacking...");
                InputStream in = getClass().getResourceAsStream("/vlc-bin/windows/" + arch + "/libvlc.dll");
                OutputStream out;
                try {
                    out = new FileOutputStream("libvlc.dll");
                    IOUtils.copy(in, out);
                    in = getClass().getResourceAsStream("/vlc-bin/windows/" + arch + "/libvlccore.dll");
                    out = new FileOutputStream("libvlccore.dll");
                    IOUtils.copy(in, out);
                    in.close();
                    out.close();
                } catch (IOException ex) {
                    ex.printStackTrace();
                }
            }
            NativeLibrary.addSearchPath(RuntimeUtil.getLibVlcLibraryName(), "");
            try {
                String path = discoverNativeVLC();
                LOGGER.info("Native VLC Found at '{}'", path);
            } catch (UnsatisfiedLinkError e1) {
                LOGGER.fatal("Couldn't load vlc binaries, crashing...");
            }
        } else if (SystemUtils.IS_OS_MAC) {
            LOGGER.error("Unsupported OS: Mac");
        } else if (SystemUtils.IS_OS_LINUX) {
            if (!new File("libvlc.so").isFile() || !new File("libvlc.so.12").isFile() || !new File("libvlc.so.12.0.0").isFile() || !new File("libvlccore.so").isFile() || !new File("libvlccore.so.9").isFile() || !new File("libvlccore.so.9.0.0").isFile()) {
                LOGGER.info("Couldn't load vlc binaries, unpacking...");
                InputStream in = getClass().getResourceAsStream("/vlc-bin/linux/" + arch + "/libvlc.so");
                OutputStream out;
                try {
                    out = new FileOutputStream("libvlc.dll");
                    IOUtils.copy(in, out);
                    in = getClass().getResourceAsStream("/vlc-bin/windows/" + arch + "/libvlccore.dll");
                    out = new FileOutputStream("libvlccore.dll");
                    IOUtils.copy(in, out);
                    in.close();
                    out.close();
                } catch (IOException ex) {
                    ex.printStackTrace();
                }
            }
            NativeLibrary.addSearchPath(RuntimeUtil.getLibVlcLibraryName(), "");
            try {
                String path = discoverNativeVLC();
                LOGGER.info("Native VLC Found at '{}'", path);
            } catch (UnsatisfiedLinkError e1) {
                LOGGER.fatal("Couldn't load vlc binaries, crashing...");
            }
        } else {
            LOGGER.fatal("Couldn't unpack vlc binaries, unsupported platform...", new UnsatisfiedLinkError("Can't find supported vlc binaries"));
            System.exit(-1);
        }
        return false;
    }

    private String discoverNativeVLC() {
        String nativeLibraryPath;
        discovery.discover();
        NativeDiscoveryStrategy nativeDiscoveryStrategy = discovery.successfulStrategy();
        nativeLibraryPath = discovery.discoveredPath();
        LOGGER.debug("Strategy: {}", nativeDiscoveryStrategy);
        LOGGER.debug("Path: {}", nativeLibraryPath);

        try {
            checkVersion();
        } catch (NoClassDefFoundError | Exception e) {
            throw new NativeLibraryMappingException("Failed to properly initialise the native library", e);
        }
        return nativeLibraryPath;
    }

    private void checkVersion() throws Exception {
        LibVlcVersion version = new LibVlcVersion();
        LOGGER.fatal(new Version(libvlc_get_version()));
        if (!version.isSupported()) {
            throw new Exception(String.format("Failed to find minimum required VLC version %s, found %s", version.getRequiredVersion(), version.getVersion()));
        }
    }

    @SubscribeEvent
    public void drawBackground(GuiScreenEvent.BackgroundDrawnEvent e) {
        // Maybe we can achieve faster handling using one of those and native render and composition calls:
        // RenderSystem.glBindBuffer();
        // RenderSystem.glBufferData();
        // Pointer
        // RenderSystem.bindTexture();
        renderBackground(e.getMatrixStack(), e.getGui());
    }

    private void renderBackground(MatrixStack matrixStack, Screen gui) {
        if (gui instanceof RealmsGenericErrorScreen) {
            if (!init) {
                MediaPlayer.newMediaPlayer();
                MediaPlayers.getPlayer(0).play("https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4");
                MediaPlayers.getPlayer(0).volume(200);
                init = true;
            }
            if (frameNumb != 0 & 0 == frameNumb % 10000) {
                MediaPlayers.getPlayer(0).pause();
            }
            frameNumb++;
            MediaPlayers.getPlayer(0).render(matrixStack, 0, 0);
//            int[] frame = MediaPlayers.getPlayer(0).getFrame();
//            BufferToMatrixStack bufferStack = new BufferToMatrixStack(matrixStack);
//            IntStream.range(0, frame.length).forEach(index -> {
//                int y = index / 1280;
//                int x = index % 1280;
//                bufferStack.set(x, y, frame[index]);
//            });
//            bufferStack.finishDrawing();
        }
        if (!(gui instanceof OptionsScreen)) {
            return;
        }
        if (!init) {
            LOGGER.info("Start playing");
            mediaPlayerComponent.mediaPlayer().media().play("https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4");
            init = true;
        }
        BufferToMatrixStack bufferStack = new BufferToMatrixStack(matrixStack);
        try {
            semaphore.acquire();
            IntStream.range(0, frame.length).forEach(index -> {
                int y = index / width;
                int x = index % width;
                bufferStack.set(x, y, frame[index]);
            });
            semaphore.release();
        } catch (InterruptedException e) {
            e.printStackTrace();
            Thread.currentThread().interrupt();
        }
        bufferStack.finishDrawing();
    }
}

package nick1st.fancyvideo;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.realmsclient.gui.screens.RealmsGenericErrorScreen;
import com.sun.jna.NativeLibrary;
import net.minecraft.client.gui.screen.OptionsScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.GuiScreenEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ExtensionPoint;
import net.minecraftforge.fml.InterModComms;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.InterModEnqueueEvent;
import net.minecraftforge.fml.event.lifecycle.InterModProcessEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.loading.FMLEnvironment;
import net.minecraftforge.fml.network.FMLNetworkConstants;
import nick1st.fancyvideo.api.MediaPlayer;
import nick1st.fancyvideo.api.MediaPlayers;
import org.apache.commons.compress.utils.IOUtils;
import org.apache.commons.lang3.SystemUtils;
import org.apache.commons.lang3.tuple.Pair;
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
@Mod("fancyvideo-api")
public class FancyVideoAPI {

    // Constants
    public static final String PLUGINSDIR = "plugins/";


    private final CallbackMediaPlayerComponent mediaPlayerComponent;
    private final VideoRenderCallback defaultRenderCallback = new VideoRenderCallback();

    // Synced objects
    static Semaphore semaphore = new Semaphore(1, true);
    static int[] frame = new int[0];
    static int width;

    private boolean init;

    private final NativeDiscovery discovery = new NativeDiscovery();

    // Directly reference a log4j logger.
    private static final Logger LOGGER = LogManager.getLogger("FancyVideo-API");

    // Temp Objects only for testing
    int frameNumb = 0;

    public FancyVideoAPI() {
        // Client only
        ModLoadingContext.get().registerExtensionPoint(ExtensionPoint.DISPLAYTEST, () -> Pair.of(() -> FMLNetworkConstants.IGNORESERVERONLY, (a, b) -> true));
        if (FMLEnvironment.dist == Dist.DEDICATED_SERVER) {
            LOGGER.warn("## WARNING ## 'FancyVideo API' is a client mod and has no effect when loaded on a server!");
        }

        // Init natives
        if (!onInit()) {
            System.exit(-9515);
        }

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

    private boolean onInit() {
        if (!new File(LibraryMapping.libVLC.linuxName).isFile() && !new File(LibraryMapping.libVLC.windowsName).isFile() &&  !new File(LibraryMapping.libVLC.macName).isFile()) {
            LOGGER.info("Unpacking natives...");
            if (!unpack()) {
                LOGGER.warn("We do not bundle natives for your os. You can try to manually install VLC Player or libVLC for your System. FancyVideo-API only runs with libVLC Versions 4.0.0+");
            }
        }
        NativeLibrary.addSearchPath(RuntimeUtil.getLibVlcLibraryName(), "");
        try {
            String path = discoverNativeVLC();
            LOGGER.info("Native VLC Found at '{}'", path);
            return true;
        } catch (UnsatisfiedLinkError e1) {
            LOGGER.fatal("Couldn't load vlc binaries, crashing...");
            return false;
        }
    }
    
    private boolean unpack() {
        // Get our os
        Map<String, String> archMap = new HashMap<>();
        archMap.put("x86", "x86");
        archMap.put("i386", "x86");
        archMap.put("i486", "x86");
        archMap.put("i586", "x86");
        archMap.put("i686", "x86");
        archMap.put("x86_64", "amd64");
        archMap.put("amd64", "amd64");
        archMap.put("arm64", "arm64");
        archMap.put("powerpc", "ppc");
        String arch = archMap.get(SystemUtils.OS_ARCH);
        String os = null;
        if (SystemUtils.IS_OS_LINUX) {
            os = "linux";
        } else if (SystemUtils.IS_OS_MAC) {
            os = "mac";
        } else if (SystemUtils.IS_OS_WINDOWS) {
            os = "windows";
        }

        ClassLoader loader = this.getClass().getClassLoader();

        // Check if we package this os and arch
        String path = "vlc-bin/" + os + "/" + arch + "/";
        LOGGER.debug(path);
        if (os == null || loader.getResource(path) == null) {
            return false;
        }

        // Extract natives
        for (LibraryMapping mapping: LibraryMapping.values()) {
            String file;
            switch (os) {
                case ("linux"):
                    file = mapping.linuxName;
                    break;
                case ("mac"):
                    file = mapping.macName;
                    break;
                case ("windows"):
                    file = mapping.windowsName;
                    break;
                default:
                    return false;
            }
            try {
                new File(PLUGINSDIR).mkdir();
                extract(loader, path, file, mapping.isPlugin);
            } catch (IOException e) {
                LOGGER.error("An error occurred whilst trying to unpack natives ", e);
            }
        }
        return true;
    }

    private void extract(ClassLoader loader, String path, String file, boolean isPlugin) throws IOException {
        if (isPlugin) {
            new File(PLUGINSDIR + file).getParentFile().mkdirs();
        }
        InputStream in = isPlugin ? loader.getResourceAsStream(path + PLUGINSDIR + file) : loader.getResourceAsStream(path + file);
        OutputStream out = isPlugin ? new FileOutputStream(PLUGINSDIR + file) : new FileOutputStream(file);
        IOUtils.copy(in, out);
        in.close();
        out.flush();
        out.close();
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
        } catch (LinkageError e) {
            throw new NativeLibraryMappingException("Failed to properly initialise the native library", e);
        }
        return nativeLibraryPath;
    }

    private void checkVersion() throws LinkageError {
        LibVlcVersion version = new LibVlcVersion();
        LOGGER.debug(new Version(libvlc_get_version()));
        if (!version.isSupported()) {
            throw new LinkageError(String.format("Failed to find minimum required VLC version %s, found %s", version.getRequiredVersion(), version.getVersion()));
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
            if (frameNumb != 0 && 0 == frameNumb % 10000) {
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

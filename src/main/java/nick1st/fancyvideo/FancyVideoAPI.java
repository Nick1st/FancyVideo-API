package nick1st.fancyvideo;

import com.sun.jna.NativeLibrary; //NOSONAR This class doesn't exist in the java api
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.fml.ExtensionPoint;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.loading.FMLEnvironment;
import net.minecraftforge.fml.network.FMLNetworkConstants;
import nick1st.fancyvideo.api.EmptyMediaPlayer;
import nick1st.fancyvideo.test.MatrixStackRenderTest;
import org.apache.commons.compress.utils.IOUtils;
import org.apache.commons.lang3.SystemUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import uk.co.caprica.vlcj.binding.RuntimeUtil;
import uk.co.caprica.vlcj.factory.NativeLibraryMappingException;
import uk.co.caprica.vlcj.factory.discovery.NativeDiscovery;
import uk.co.caprica.vlcj.factory.discovery.strategy.NativeDiscoveryStrategy;
import uk.co.caprica.vlcj.support.version.LibVlcVersion;
import uk.co.caprica.vlcj.support.version.Version;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.util.HashMap;
import java.util.Map;

import static nick1st.fancyvideo.Constants.AMD_64;
import static nick1st.fancyvideo.Constants.PLUGINSDIR;
import static uk.co.caprica.vlcj.binding.LibVlc.libvlc_get_version;

// The value here should match an entry in the META-INF/mods.toml file
@Mod("fancyvideo-api")
public class FancyVideoAPI {

    // Running DEBUG?
    private static final boolean DEBUG = true;
    private MatrixStackRenderTest matrixRenderTest;

    private final NativeDiscovery discovery = new NativeDiscovery();

    // Directly reference a log4j logger.
    public static final Logger LOGGER = LogManager.getLogger("FancyVideo-API");

    // First render Tick
    private boolean renderTick;

    public FancyVideoAPI() {
        // Client only
        ModLoadingContext.get().registerExtensionPoint(ExtensionPoint.DISPLAYTEST, () -> Pair.of(() -> FMLNetworkConstants.IGNORESERVERONLY, (a, b) -> true));
        if (FMLEnvironment.dist == Dist.DEDICATED_SERVER) {
            LOGGER.warn("## WARNING ## 'FancyVideo API' is a client mod and has no effect when loaded on a server!");
            return;
        }

        // Init natives
        if (!onInit()) {
            System.exit(-9515);
        }

        // Setup API
//        EmptyMediaPlayer.getInstance();

        // Debug?
        if (DEBUG) {
            MinecraftForge.EVENT_BUS.addListener(this::renderTick);
        }

        // Register ourselves for server and other game events we are interested in
        MinecraftForge.EVENT_BUS.register(this);
    }

    public void renderTick(TickEvent.RenderTickEvent event) {
        if (!renderTick) {
            LOGGER.info("Tick");
            matrixRenderTest = new MatrixStackRenderTest();
            matrixRenderTest.init();
            MinecraftForge.EVENT_BUS.addListener(matrixRenderTest::drawBackground);
            renderTick = true;
        }
    }

    private boolean onInit() {
        deleteOldLog();
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
        archMap.put("x86_64", AMD_64);
        archMap.put(AMD_64, AMD_64);
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
                //noinspection ResultOfMethodCallIgnored
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
            //noinspection ResultOfMethodCallIgnored
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

    private void deleteOldLog() {
        try {
            Files.delete(new File("logs/vlc.log").toPath());
        } catch (NoSuchFileException ignored) {
            // Ignored
        } catch (IOException e) {
            e.printStackTrace();
        }

    }
}

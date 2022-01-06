package nick1st.fancyvideo;

import com.sun.jna.NativeLibrary;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.ExtensionPoint;
import net.minecraftforge.fml.InterModComms;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.InterModEnqueueEvent;
import net.minecraftforge.fml.event.lifecycle.InterModProcessEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.loading.FMLEnvironment;
import net.minecraftforge.fml.network.FMLNetworkConstants;
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
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Semaphore;
import java.util.stream.Collectors;

import static uk.co.caprica.vlcj.binding.LibVlc.libvlc_get_version;

// The value here should match an entry in the META-INF/mods.toml file
@Mod("fancyvideo-api")
public class FancyVideoAPI {

    // Constants
    public static final String PLUGINSDIR = "plugins/";
    public static final String AMD_64 = "amd64";

    // Running DEBUG log generation?
    private static final boolean DEBUG = true;
    private final MatrixStackRenderTest matrixRenderTest;

    // Synced objects
    static Semaphore semaphore = new Semaphore(1, true);
    static int[] frame = new int[0];
    static int width;

    private final NativeDiscovery discovery = new NativeDiscovery();

    // Directly reference a log4j logger.
    public static final Logger LOGGER = LogManager.getLogger("FancyVideo-API");

    public FancyVideoAPI() {
        //Native.loadLibrary((Platform.isWindows() ? "msvcrt" : "c"), LibC.class);
        //LibC.INSTANCE = Native.loadLibrary((Platform.isWindows() ? "msvcrt" : "c"), LibC.class);

        // Temp JNA Fix?
        // System.setProperty("jna.nosys", "true");

        // Client only
        ModLoadingContext.get().registerExtensionPoint(ExtensionPoint.DISPLAYTEST, () -> Pair.of(() -> FMLNetworkConstants.IGNORESERVERONLY, (a, b) -> true));
        if (FMLEnvironment.dist == Dist.DEDICATED_SERVER) {
            LOGGER.warn("## WARNING ## 'FancyVideo API' is a client mod and has no effect when loaded on a server!");
        }

        // Init natives
        if (!onInit()) {
            System.exit(-9515);
        }

        // Debug?
        if (DEBUG) {
            matrixRenderTest = new MatrixStackRenderTest();
            matrixRenderTest.init();
            MinecraftForge.EVENT_BUS.addListener(matrixRenderTest::drawBackground);
        }

        // Register the enqueueIMC and processIMC method for modloading
        FMLJavaModLoadingContext.get().getModEventBus().addListener(this::enqueueIMC);
        FMLJavaModLoadingContext.get().getModEventBus().addListener(this::processIMC);

        // Register ourselves for server and other game events we are interested in
        MinecraftForge.EVENT_BUS.register(this);
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

    private void deleteOldLog() {
        new File("logs/vlc.log").delete();
    }
}

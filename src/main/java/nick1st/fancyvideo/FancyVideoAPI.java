package nick1st.fancyvideo;

import com.sun.jna.NativeLibrary; //NOSONAR This class doesn't exist in the java api
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.EventSubclassTransformer;
import net.minecraftforge.fml.ExtensionPoint;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.loading.FMLEnvironment;
import net.minecraftforge.fml.network.FMLNetworkConstants;
import nick1st.fancyvideo.api.EmptyMediaPlayer;
import nick1st.fancyvideo.api.ShutdownHook;
import nick1st.fancyvideo.config.SimpleConfig;
import nick1st.fancyvideo.test.MatrixStackRenderTest;
import org.apache.commons.compress.utils.IOUtils;
import org.apache.commons.lang3.SystemUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.filter.AbstractFilter;
import uk.co.caprica.vlcj.binding.RuntimeUtil;
import uk.co.caprica.vlcj.factory.NativeLibraryMappingException;
import uk.co.caprica.vlcj.factory.discovery.NativeDiscovery;
import uk.co.caprica.vlcj.factory.discovery.strategy.NativeDiscoveryStrategy;
import uk.co.caprica.vlcj.support.version.LibVlcVersion;
import uk.co.caprica.vlcj.support.version.Version;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import static nick1st.fancyvideo.Constants.AMD_64;
import static nick1st.fancyvideo.Constants.PLUGINSDIR;
import static uk.co.caprica.vlcj.binding.LibVlc.libvlc_get_version;

// The value here should match an entry in the META-INF/mods.toml file
@Mod("fancyvideo-api")
public class FancyVideoAPI {

    // Directly reference a log4j logger.
    public static final Logger LOGGER = LogManager.getLogger("FancyVideo-API");
    private final NativeDiscovery discovery = new NativeDiscovery();

    // DEBUG VAR
    private MatrixStackRenderTest matrixRenderTest;
    // First render Tick
    private boolean renderTick;
    // Config Holder
    private SimpleConfig config;
    private final int dllVersion = 0;

    public FancyVideoAPI() {
        // Client only
        ModLoadingContext.get().registerExtensionPoint(ExtensionPoint.DISPLAYTEST, () -> Pair.of(() -> FMLNetworkConstants.IGNORESERVERONLY, (a, b) -> true));
        if (FMLEnvironment.dist == Dist.DEDICATED_SERVER) {
            LOGGER.warn("## WARNING ## 'FancyVideo API' is a client mod and has no effect when loaded on a server!");
            return;
        }

        // Init Config
        initConfig();

        // Ignore the silly NullPointers caused by ModLauncher // TODO Make this actually STOP the error
        if (LogManager.getLogger(EventSubclassTransformer.class) instanceof org.apache.logging.log4j.core.Logger && !config.getAsBool("debugLog")) {
            org.apache.logging.log4j.core.Logger logger = (org.apache.logging.log4j.core.Logger) LogManager.getLogger(EventSubclassTransformer.class);
            logger.warn("## WARNING ## 'FancyVideo API' is modifying this log! Disable this behavior in config BEFORE reporting bugs!");
            logger.addFilter(new AbstractFilter() {
                @Override
                public Result filter(LogEvent event) {
                    if (event.getMessage() != null && event.getThrown() != null && event.getMarker() != null) {
                        if (event.getMarker().getName().equals("EVENTBUS") && event.getMessage().getFormattedMessage().equals("An error occurred building event handler")) {
                            if (Arrays.stream(event.getThrown().getStackTrace()).anyMatch(sTE -> sTE.getClassName().startsWith("uk.co.caprica.vlcj."))) {
                                // LOGGER.fatal("This is nice");
                                return Result.DENY;
                            }
                        }
                    }
                    return Result.NEUTRAL;
                }
            });

            // Test if it works
            // Throwable t = new Throwable();
            // t.setStackTrace(new StackTraceElement[]{new StackTraceElement("uk.co.caprica.vlcj.binding.LibC", "free", "LibC.class", 126)});
            // logger.error("An error occurred building event handler", t);
        }

        // Delete mismatched dlls
        if (config.getAsInt("dllVersion") != dllVersion) {
            LOGGER.info("DLL Version did change, removing old files...");
            // clearDLL();
            config.properties.setProperty("dllVersion", String.valueOf(dllVersion));
            config.write();
        }

        // Init natives
        if (!onInit()) {
            System.exit(-9515);
        }

        // Setup API
        EmptyMediaPlayer.getInstance();

        // Example?
        if (config.getAsBool("example")) {
            MinecraftForge.EVENT_BUS.addListener(this::renderTick);
        }

        // Register ourselves for server and other game events we are interested in
        MinecraftForge.EVENT_BUS.register(this);

        // Register shutdown hook
        Runtime.getRuntime().addShutdownHook(new ShutdownHook()); // TODO Finish this
    }

    private void initConfig() {
        // Super Simple Config
        // We can't use Forges Config, because it loads way to late
        config = new SimpleConfig(new File("config", "fancyvideo-api.cfg"));

        config.setProperty("dllVersion", String.valueOf(dllVersion), "DO NOT MODIFY THIS! (Set it to -1 to regenerate your DLLs, but otherwise DO NOT TOUCH!)", ">= -1", s -> {
            try {
                if (Integer.parseInt(s) >= -1) {
                    return true;
                }
            } catch (NumberFormatException ignored) {
            }
            return false;
        });
        config.setProperty("debugLog", String.valueOf(false), "Enable debug logging. Disables the ModLauncher log filter. This cause massive log spam! Only activate this when you're told to!", "true / false", s -> Arrays.asList("true", "false").contains(s));
        config.setProperty("example", String.valueOf(false), "Activate the debug/showcase mode. Access it by pressing the Realms Button in Main Menu.", "true / false", s -> Arrays.asList("true", "false").contains(s));

        config.read();
        config.write();
    }

    public void renderTick(TickEvent.RenderTickEvent event) {
        if (!renderTick) {
            EmptyMediaPlayer.getInstance().setUp();
            LOGGER.info("Tick");
            matrixRenderTest = new MatrixStackRenderTest();
            matrixRenderTest.init();
            MinecraftForge.EVENT_BUS.addListener(matrixRenderTest::drawBackground);
            renderTick = true;
        }
    }

    private boolean onInit() {
        deleteOldLog();
        if (!new File(LibraryMapping.libVLC.linuxName).isFile() && !new File(LibraryMapping.libVLC.windowsName).isFile() && !new File(LibraryMapping.libVLC.macName).isFile()) {
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
        for (LibraryMapping mapping : LibraryMapping.values()) {
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

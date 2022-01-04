package nick1st.fancyvideo;

public enum LibraryMapping {
    // core
    libVLC("libvlc", false),
    libVLCCore("libvlccore", false),

    // audio_filter
    libEqualizer("audio_filter", "libequalizer_plugin"),

    // audio_output
    libADummy("audio_output", "libadummy_plugin"),
    libAMem("audio_output", "libamem_plugin"),
    libDirectSound("audio_output", "libdirectsound_plugin"),

    // logger
    libConsoleLogger("logger", "libconsole_logger_plugin"),

    // spu
    libLogo("spu", "liblogo_plugin"),
    libMarq("spu", "libmarq_plugin"),

    // video_filter // TODO: Find out if we need all of those
    libAdjust("video_filter", "libadjust_plugin"),
    libAlphaMask("video_filter", "libalphamask_plugin"),
    libDeinterlace("video_filter", "libdeinterlace_plugin"),
    libFPS("video_filter", "libfps_plugin"),

    // video_output
    libVDummy("video_output", "libwdummy_plugin"),
    libVMem("video_output", "libvmem_plugin");


    String windowsName;
    String linuxName;
    String macName;
    boolean isPlugin;

    LibraryMapping(String windowsName, String linuxName, String macName, boolean isPlugin) {
        this.windowsName = windowsName;
        this.linuxName = linuxName;
        this.macName = macName;
        this.isPlugin = isPlugin;
    }

    LibraryMapping(String windowsName, String linuxName, String macName) {
        this(windowsName, linuxName, macName, true);
    }

    LibraryMapping(String simpleName, boolean isPlugin) {
        this(simpleName + ".dll", simpleName + ".so", simpleName + ".dylib", isPlugin);
    }

    LibraryMapping(String prefix, String simpleName) {
        this(prefix + "/" + simpleName, true);
    }

    LibraryMapping(String simpleName) {
        this(simpleName, true);
    }
}


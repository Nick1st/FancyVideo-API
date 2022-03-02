package nick1st.fancyvideo.api;


import nick1st.fancyvideo.FancyVideoAPI;

public class ShutdownHook extends Thread {

    private final MediaPlayers instance = MediaPlayers.getInstance();

    @Override
    public void run() {
        FancyVideoAPI.LOGGER.info("Running FancyVideo-API shutdown hook");
        instance.shutdown();
        FancyVideoAPI.LOGGER.info("Shutdown hook finished");
    }

}

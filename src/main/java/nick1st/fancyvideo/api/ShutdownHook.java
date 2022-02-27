package nick1st.fancyvideo.api;


public class ShutdownHook extends Thread {

    public final MediaPlayers lock;

    public ShutdownHook(MediaPlayers lock) {
        this.lock = lock;
    }

    @Override
    public void run() {
        System.out.println("Running FancyVideo-API shutdown hook");
        //MediaPlayers.shutdown(lock);
        System.out.println("Shutdown hook finished");
    }


}

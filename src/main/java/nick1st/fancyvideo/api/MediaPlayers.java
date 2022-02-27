package nick1st.fancyvideo.api;

import uk.co.caprica.vlcj.factory.MediaPlayerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * This class manages the media players. <br>
 * Most methods are for internal use only. Use {@link MediaPlayer#getNew()} to create a new {@link MediaPlayer} and get its unique id.
 * Use {@link #getPlayer(int)} to get your reference afterwards. <br>
 * If your player is not required anymore, use {@link MediaPlayer#destroy()} to free its resources. Calling {@link #getPlayer(int)} on a removed or
 * not existing player may <b>throw a {@link IndexOutOfBoundsException} or return {@link null}</b>. Use {@link #isValid(int)} to check if your reference is valid. <br>
 * Your object may get removed if a {link ShutdownEvent} fires, as this {@link #shutdown(MediaPlayers)} the library.
 */
public final class MediaPlayers { // TODO Make this garbage collection save

    private static MediaPlayers instance;
    private final List<MediaPlayer> playerStore = new ArrayList<>();
    boolean shutdown = false;
    MediaPlayerFactory factory = new MediaPlayerFactory("--no-metadata-network-access", "--file-logging", "--logfile", "logs/vlc.log", "--logmode", "text", "--verbose", "2", "--no-quiet");

    private MediaPlayers() {
    }

    public static synchronized MediaPlayers getInstance() {
        if (MediaPlayers.instance == null) {
            MediaPlayers.instance = new MediaPlayers();
        }
        return MediaPlayers.instance;
    }

    static synchronized int addPlayer(MediaPlayer player) {
        if (!instance.playerStore.contains(player)) {
            instance.playerStore.add(player);
        }
        return instance.playerStore.indexOf(player);
    }

    public static synchronized void removePlayer(int id) {
        getPlayer(id).destroy();
        instance.playerStore.set(id, EmptyMediaPlayer.getInstance());
    }

    public static MediaPlayer getPlayer(int id) {
        return instance.playerStore.get(id);
    }

    public static boolean isValid(int id) {
        if (instance.playerStore.size() < id) {
            return false;
        } else return instance.playerStore.get(id) != EmptyMediaPlayer.getInstance();
    }

    public static void shutdown() { //TODO Nonpublic and finish this
        if (!instance.shutdown) {
            instance.shutdown = true;
        } else {
            return;
        }
        System.out.println("Running shutdown");
        instance.playerStore.forEach(MediaPlayer::destroy);
        System.out.println("Running shutdown finished");
        instance.playerStore.clear();
        instance.factory.release();
    }
}

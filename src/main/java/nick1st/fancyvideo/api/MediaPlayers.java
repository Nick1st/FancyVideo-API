package nick1st.fancyvideo.api;

import uk.co.caprica.vlcj.factory.MediaPlayerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * This class manages the media players. <br>
 * Most methods are for internal use only. Use {@link MediaPlayer#newMediaPlayer()} to create a new {@link MediaPlayer} and get its unique id.
 * Use {@link #getPlayer(int)} to get your reference afterwards. <br>
 * If your player is not required anymore, use {@link MediaPlayer#destroy()} to free its resources. Calling {@link #getPlayer(int)} on a removed or
 * not existing player may throw a {@link IndexOutOfBoundsException} or return {@link null}. Use {@link #isValid(int)} to check if your reference is valid. <br>
 * Your object may get removed if a {link ShutdownEvent} fires, as this {@link #shutdown()} the library.
 */
public final class MediaPlayers {

    private static final List<MediaPlayer> playerStore = new ArrayList<>();
    static MediaPlayerFactory factory = new MediaPlayerFactory("--no-metadata-network-access");

    private MediaPlayers() {
    }

    static synchronized int addPlayer(MediaPlayer player) {
        if (!playerStore.contains(null)) {
            playerStore.add(player);
        } else {
            playerStore.add(playerStore.indexOf(null), player);
        }
        return playerStore.indexOf(player);
    }

    static synchronized void destroy(int id) {
        playerStore.set(id, null);
    }

    public static MediaPlayer getPlayer(int id) {
        return playerStore.get(id);
    }

    public static boolean isValid(int id) {
        if (playerStore.size() < id) {
            return false;
        } else return playerStore.get(id) != null;
    }

    static synchronized void shutdown() {
        playerStore.forEach(MediaPlayer::destroy);
        playerStore.forEach(playerStore::remove);
        factory.release();
    }
}

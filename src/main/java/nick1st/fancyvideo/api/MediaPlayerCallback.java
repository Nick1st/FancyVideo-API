package nick1st.fancyvideo.api;

import uk.co.caprica.vlcj.player.embedded.videosurface.callback.RenderCallbackAdapter;

public class MediaPlayerCallback extends RenderCallbackAdapter  {
    private int width;
    private final MediaPlayer mediaPlayer;

    public MediaPlayerCallback(int width, MediaPlayer mediaPlayer) {
        this.width = width;
        this.mediaPlayer = mediaPlayer;
    }

    protected void setBuffer(int width, int[] buffer) {
        this.width = width;
        setBuffer(buffer);
    }

    @Override
    protected void onDisplay(uk.co.caprica.vlcj.player.base.MediaPlayer mediaPlayer, int[] buffer) {
        this.mediaPlayer.setFrame(buffer.clone(), width);
    }
}

package nick1st.fancyvideo.api;

import uk.co.caprica.vlcj.player.embedded.videosurface.callback.RenderCallbackAdapter;

public class MediaPlayerCallback extends RenderCallbackAdapter {
    private final MediaPlayer mediaPlayer;
    private int width;

    public MediaPlayerCallback(int width, MediaPlayer mediaPlayer) {
        this.width = width;
        this.mediaPlayer = mediaPlayer;
    }

    void setBuffer(AdvancedFrame buffer) {
        this.width = buffer.width;
        setBuffer(buffer.frame);
    }

    @Override
    protected void onDisplay(uk.co.caprica.vlcj.player.base.MediaPlayer mediaPlayer, int[] buffer) {
        this.mediaPlayer.setFrame(new AdvancedFrame(buffer, width));
    }
}

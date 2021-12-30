package nick1st.fancyvideo.api;

import uk.co.caprica.vlcj.player.base.MediaPlayer;
import uk.co.caprica.vlcj.player.embedded.videosurface.callback.RenderCallbackAdapter;

public class MediaPlayerCallback extends RenderCallbackAdapter  {
    private int width;
    private int height;
    private nick1st.fancyvideo.api.MediaPlayer mediaPlayer;

    public MediaPlayerCallback(int width, int height, nick1st.fancyvideo.api.MediaPlayer mediaPlayer) {
        this.width = width;
        this.height = height;
        this.mediaPlayer = mediaPlayer;
    }

    protected void setBuffer(int width, int height, int[] buffer) {
        this.width = width;
        this.height = height;
        setBuffer(buffer);
    }

    @Override
    protected void onDisplay(MediaPlayer mediaPlayer, int[] buffer) {
        this.mediaPlayer.setFrame(buffer.clone(), width);
    }
}

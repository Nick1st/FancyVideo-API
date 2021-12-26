package nick1st.vlcjtest;

import uk.co.caprica.vlcj.player.base.MediaPlayer;
import uk.co.caprica.vlcj.player.embedded.videosurface.callback.RenderCallbackAdapter;

import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;
import java.util.Arrays;
import java.util.stream.Collectors;

public class VideoRenderCallback extends RenderCallbackAdapter {

    private int width;

    void setImageBuffer(BufferedImage image) {
        width = image.getWidth();
        setBuffer(((DataBufferInt) image.getRaster().getDataBuffer()).getData());
    }

    @Override
    protected void onDisplay(MediaPlayer mediaPlayer, int[] buffer) {
        try {
            VlcJTest.semaphore.acquire();
            VlcJTest.frameList = Arrays.stream(buffer).boxed().collect(Collectors.toList());
            VlcJTest.width = width;
            VlcJTest.semaphore.release();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}

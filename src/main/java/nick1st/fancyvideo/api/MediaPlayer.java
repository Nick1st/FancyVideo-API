package nick1st.fancyvideo.api;

import com.mojang.blaze3d.matrix.MatrixStack;
import nick1st.fancyvideo.BufferToMatrixStack;
import uk.co.caprica.vlcj.player.component.CallbackMediaListPlayerComponent;
import uk.co.caprica.vlcj.player.component.CallbackMediaPlayerComponent;
import uk.co.caprica.vlcj.player.embedded.videosurface.callback.BufferFormat;
import uk.co.caprica.vlcj.player.embedded.videosurface.callback.BufferFormatCallbackAdapter;
import uk.co.caprica.vlcj.player.embedded.videosurface.callback.format.RV32BufferFormat;

import java.nio.ByteBuffer;
import java.util.concurrent.Semaphore;
import java.util.stream.IntStream;

public final class MediaPlayer {
    // Important stuff
    private CallbackMediaPlayerComponent mediaPlayer;
    private MediaPlayerCallback callback = new MediaPlayerCallback(0, 0, this);
    private final int id;

    // The last rendered frame is stored here
    private int[] frame = new int[0];
    private int width;
    private ByteBuffer byteBuffer = null;
    private Semaphore semaphore = new Semaphore(1, true);

    private MediaPlayer() {
        mediaPlayer = new CallbackMediaListPlayerComponent(MediaPlayers.factory, null, null, true, null, callback, new DefaultBufferFormatCallback(), null);
        id = MediaPlayers.addPlayer(this);
    }

    /**
     * Creates a new MediaPlayer for you. Call {@link MediaPlayers#getPlayer(int)} to get your reference
     * Please call {@link #destroy()} when you don't need it anymore
     * @return ID of the new MediaPlayer (keep it, it's important!)
     */
    public static int newMediaPlayer() {
        return new MediaPlayer().id;
    }

    public void destroy() {
        mediaPlayer.release();
        MediaPlayers.destroy(id);
    }

    public void play(String mrl, String... options) {
        mediaPlayer.mediaPlayer().media().play(mrl, options);
    }

    /**
     *
     * @param percentage Reaches 0 - 200
     */
    public void volume(int percentage) {
        mediaPlayer.mediaPlayer().audio().setVolume(percentage);
    }

    public void mute() {
        mediaPlayer.mediaPlayer().audio().mute();
    }

    public void prepare(String mrl, String... options) {
        mediaPlayer.mediaPlayer().media().prepare(mrl, options);
    }

    public void pause() {
        mediaPlayer.mediaPlayer().controls().pause();
    }

    public int[] getFrame() {
        try {
            semaphore.acquire();
            int[] currentFrame = frame.clone();
            semaphore.release();
            return currentFrame;
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return new int[0];
    }

    public AdvancedFrameData getFrameAdvanced() {
        try {
            semaphore.acquire();
            AdvancedFrameData currentFrame = new AdvancedFrameData(frame.clone(), width);
            semaphore.release();
            return currentFrame;
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return new AdvancedFrameData(new int[0], 0);
    }

    void setFrame(int[] frame, int width) {
        try {
            semaphore.acquire();
            this.frame = frame;
            this.width = width;
            semaphore.release();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    void setByteBuffer(ByteBuffer byteBuffer) {
        try {
            semaphore.acquire();
                this.byteBuffer = byteBuffer;
            semaphore.release();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    /**
     *
     * @return This returns the true MediaPlayer, allowing you to use (nearly) all functions of libvlc.
     * Only use when you know what you're doing.
     */
    public CallbackMediaPlayerComponent getTrueMediaPlayer() {
        return mediaPlayer;
    }

    public MatrixStack render(MatrixStack matrixStack, int x, int y) {
        AdvancedFrameData frameAdvanced = getFrameAdvanced();
        int[] frame = frameAdvanced.frame;
        int width = frameAdvanced.width;
        System.out.println(frame.length + " | " + width);
        BufferToMatrixStack bufferStack = new BufferToMatrixStack(matrixStack);
        IntStream.range(0, frame.length).forEach(index -> {
            bufferStack.set(index % width + x, index / width + y, frame[index]);
        });
        bufferStack.finishDrawing();
        //Minecraft.getInstance().getTextureManager().getDynamicTextureLocation("Test", new SelfcleaningDynamicTexture(new NativeImage()));
        //AbstractGui.blit(matrixStack, 10, 10, 10, 0F, 0F, 64, 64, 64, 64);
        return matrixStack;
    }


    /**
     * Default implementation of a buffer format callback that returns a buffer format suitable for rendering RGB frames
     */
    private class DefaultBufferFormatCallback extends BufferFormatCallbackAdapter {
        @Override
        public BufferFormat getBufferFormat(int sourceWidth, int sourceHeight) {
            System.out.println(sourceWidth + " | " + sourceHeight);
            width = sourceWidth;
            callback.setBuffer(sourceWidth, sourceHeight, new int[sourceWidth * sourceHeight]);
            return new RV32BufferFormat(sourceWidth, sourceHeight);
        }
    }
}

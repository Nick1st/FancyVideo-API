package nick1st.fancyvideo.api;

import com.mojang.blaze3d.matrix.MatrixStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.NativeImage;
import net.minecraft.util.ResourceLocation;
import nick1st.fancyvideo.BufferToMatrixStack;
import nick1st.fancyvideo.FancyVideoAPI;
import uk.co.caprica.vlcj.player.component.CallbackMediaListPlayerComponent;
import uk.co.caprica.vlcj.player.component.CallbackMediaPlayerComponent;
import uk.co.caprica.vlcj.player.embedded.videosurface.callback.BufferFormat;
import uk.co.caprica.vlcj.player.embedded.videosurface.callback.BufferFormatCallbackAdapter;
import uk.co.caprica.vlcj.player.embedded.videosurface.callback.format.RV32BufferFormat;

import java.util.concurrent.Semaphore;
import java.util.stream.IntStream;

/**
 * Main class of the MediaPlayerAPI.
 * Call {@link #getNew()} to create a new media player. <br>
 * Use {@link MediaPlayers#getPlayer(int)} to get the reference of your player. <br>
 * Please call {@link #destroy()} when you don't need it anymore. <br>
 * <b>Your player may get delete on certain event calls (e.g. {link ShutdownEvent}). You need to be aware of this. </b>
 * <b>Use {@link MediaPlayers#isValid(int)} to check if the player can be used.</b>
 */
public class MediaPlayer {
    // Important stuff
    private final CallbackMediaPlayerComponent mediaPlayerComponent;
    private final MediaPlayerCallback callback = new MediaPlayerCallback(0, this);
    private final int id;

    // The last rendered frame is stored here
    private AdvancedFrame videoStream = new AdvancedFrame(new int[0], 0);
    private final Semaphore semaphore = new Semaphore(1, true);

    // Image
    private NativeImage image = new NativeImage(1, 1, true);
    private final SelfCleaningDynamicTexture dyTex = new SelfCleaningDynamicTexture(image);
    private final ResourceLocation loc;

    MediaPlayer() {
        mediaPlayerComponent = new CallbackMediaListPlayerComponent(MediaPlayers.factory, null, null, true, null, callback, new DefaultBufferFormatCallback(), null);
        id = MediaPlayers.addPlayer(this);
        init();
        loc = Minecraft.getInstance().getTextureManager().register("video_texture" + id, dyTex);
    }

    /**
     * Creates a new MediaPlayer for you. Call {@link MediaPlayers#getPlayer(int)} to get your reference
     * Please call {@link #destroy()} when you don't need it anymore
     * @return ID of the new MediaPlayer (keep it, it's important!)
     */
    public static int getNew() {
        return new MediaPlayer().id;
    }

    /**
     * Init an empty frame (Hex: 0x000000; Alpha: 0x00)
     */
    public void init() {
        image = new NativeImage(1, 1, true);
        image.setPixelRGBA(0, 0, 0);
        dyTex.setPixels(image);
    }

    public void destroy() {
        mediaPlayerComponent.release();
        MediaPlayers.removePlayer(id);
    }

    public void play(String mrl, String... options) {
        mediaPlayerComponent.mediaPlayer().media().play(mrl, options);
    }

    /**
     * @param percentage Reaches 0 - 200
     */
    public void volume(int percentage) {
        mediaPlayerComponent.mediaPlayer().audio().setVolume(percentage);
    }

    public void mute() {
        mediaPlayerComponent.mediaPlayer().audio().mute();
    }

    public void prepare(String mrl, String... options) {
        mediaPlayerComponent.mediaPlayer().media().prepare(mrl, options);
    }

    public void playPrepared() {
        mediaPlayerComponent.mediaPlayer().controls().play();
    }

    public void preparePaused(String mrl, String... options) {
        mediaPlayerComponent.mediaPlayer().media().startPaused(mrl, options);
    }

    public void pause() {
        mediaPlayerComponent.mediaPlayer().controls().pause();
    }

    public int[] getFrame() {
        try {
            semaphore.acquire();
            int[] currentFrame = new AdvancedFrame(videoStream).frame;
            semaphore.release();
            return currentFrame;
        } catch (InterruptedException e) {
            e.printStackTrace();
            Thread.currentThread().interrupt();
        }
        return new int[0];
    }

    public AdvancedFrame getFrameAdvanced() {
        try {
            semaphore.acquire();
            AdvancedFrame currentFrame = new AdvancedFrame(videoStream);
            semaphore.release();
            return currentFrame;
        } catch (InterruptedException e) {
            e.printStackTrace();
            Thread.currentThread().interrupt();
        }
        return new AdvancedFrame(new int[0], 0);
    }

    void setFrame(AdvancedFrame in) {
        try {
            semaphore.acquire();
            videoStream = new AdvancedFrame(in);
            semaphore.release();
        } catch (InterruptedException e) {
            e.printStackTrace();
            Thread.currentThread().interrupt();
        }
    }

    /**
     * @return This returns the true {@link CallbackMediaListPlayerComponent}, allowing you to use (nearly) all functions of libvlc.
     * Only use when you know what you're doing.
     */
    public CallbackMediaPlayerComponent getTrueMediaPlayer() {
        return mediaPlayerComponent;
    }

    public MatrixStack render(MatrixStack matrixStack, int x, int y) {
        AdvancedFrame frameAdvanced = getFrameAdvanced();
        int[] frame = frameAdvanced.frame;
        int width = frameAdvanced.width;
        BufferToMatrixStack bufferStack = new BufferToMatrixStack(matrixStack);
        IntStream.range(0, frame.length).forEach(index -> bufferStack.set(index % width + x, index / width + y, frame[index]));
        bufferStack.finishDrawing();
        return matrixStack;
    }

    /**
     * Renders the current frame to a {@link ResourceLocation} for further use.
     * @return The {@link ResourceLocation} rendered to.
     */
    public ResourceLocation renderImage() {
        AdvancedFrame frameAdvanced = getFrameAdvanced();
        int[] frame = frameAdvanced.frame;
        int width = frameAdvanced.width;
        if (width == 0)  {
            return loc;
        }
        image = new NativeImage(width, frame.length / width, true);
        IntStream.range(0, frame.length).forEach(index -> {
            int x = index % width;
            int y = index / width;

            int color = frame[index];
            color <<= 8;
            color |= 0xFF;
            color = Integer.reverseBytes(color);

            image.setPixelRGBA(x, y, color);
        });
        dyTex.setPixels(image);
        return loc;
    }

    /**
     * Binds the current frame for further use.
     */
    public void bindFrame() {
        Minecraft.getInstance().textureManager.bind(renderImage());
    }


    /**
     * Default implementation of a buffer format callback that returns a buffer format suitable for rendering RGB frames
     */
    private class DefaultBufferFormatCallback extends BufferFormatCallbackAdapter {
        @Override
        public BufferFormat getBufferFormat(int sourceWidth, int sourceHeight) {
            FancyVideoAPI.LOGGER.info("Dimensions of player {}: {} | {}", id, sourceWidth, sourceHeight);
            callback.setBuffer(new AdvancedFrame(new int[sourceWidth * sourceHeight], sourceWidth));
            return new RV32BufferFormat(sourceWidth, sourceHeight);
        }
    }
}

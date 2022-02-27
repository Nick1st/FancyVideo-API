package nick1st.fancyvideo.api;

import com.mojang.blaze3d.matrix.MatrixStack;
import net.minecraft.client.Minecraft;
import net.minecraft.util.ResourceLocation;
import uk.co.caprica.vlcj.player.component.CallbackMediaListPlayerComponent;
import uk.co.caprica.vlcj.player.component.CallbackMediaPlayerComponent;

public final class EmptyMediaPlayer extends MediaPlayer {

    private static final AdvancedFrame emptyAdvancedFrame = new AdvancedFrame(new int[0], 0);
    private static EmptyMediaPlayer instance;

    private EmptyMediaPlayer() {
        super();
        this.destroy();
        mediaPlayerComponent = null;
        //mediaPlayerComponent = new CallbackMediaListPlayerComponent(MediaPlayers.getInstance().factory, null, null, true, null, null, null, null);
    }

    public static synchronized EmptyMediaPlayer getInstance() {
        if (EmptyMediaPlayer.instance == null) {
            EmptyMediaPlayer.instance = new EmptyMediaPlayer();
        }
        return EmptyMediaPlayer.instance;
    }

    public void setUp() {
        super.init();
    }

    @Override
    public void init() {
        // Void Callback
    }

    @Override
    public void destroy() {
        System.out.println("Destroyed");
        mediaPlayerComponent.mediaPlayer().controls().stop();
        mediaPlayerComponent.release();
        // Void Callback
    }

    @Override
    public void play(String mrl, String... options) {
        // Void Callback
    }

    @Override
    public void volume(int percentage) {
        // Void Callback
    }

    @Override
    public void mute() {
        // Void Callback
    }

    @Override
    public void prepare(String mrl, String... options) {
        // Void Callback
    }

    @Override
    public void playPrepared() {
        // Void Callback
    }

    @Override
    public void preparePaused(String mrl, String... options) {
        // Void Callback
    }

    @Override
    public void pause() {
        // Void Callback
    }

    @Override
    public int[] getFrame() {
        return new int[0];
    }

    @Override
    public AdvancedFrame getFrameAdvanced() {
        return emptyAdvancedFrame;
    }

    @Override
    public CallbackMediaPlayerComponent getTrueMediaPlayer() throws NullPointerException {
        throw new NullPointerException("Running on EmptyMediaPlayer");
    }

    @Override
    public MatrixStack render(MatrixStack matrixStack, int x, int y) {
        return matrixStack;
    }

    @Override
    public ResourceLocation renderImage() {
        return this.loc;
    }

    @Override
    public void bindFrame() {
        Minecraft.getInstance().textureManager.bind(new ResourceLocation("minecraft", "dynamic/video_texture0_1"));
    }

}

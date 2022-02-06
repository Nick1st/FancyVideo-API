package nick1st.fancyvideo.api;

import com.mojang.blaze3d.matrix.MatrixStack;
import net.minecraft.util.ResourceLocation;
import uk.co.caprica.vlcj.player.component.CallbackMediaListPlayerComponent;
import uk.co.caprica.vlcj.player.component.CallbackMediaPlayerComponent;

public final class EmptyMediaPlayer extends MediaPlayer{

    private static EmptyMediaPlayer instance;

    // Important stuff
    private final CallbackMediaPlayerComponent mediaPlayer;

    private EmptyMediaPlayer() {
        mediaPlayer = new CallbackMediaListPlayerComponent(MediaPlayers.factory, null, null, true, null, null, null, null);
        MediaPlayers.addPlayer(this);
    }

    public static synchronized EmptyMediaPlayer getInstance () {
        if (EmptyMediaPlayer.instance == null) {
            EmptyMediaPlayer.instance = new EmptyMediaPlayer();
        }
        return EmptyMediaPlayer.instance;
    }

    @Override
    public void init() {
        // Void Callback
    }

    @Override
    public void destroy() {
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
        return new AdvancedFrame(new int[0], 0);
    }

    @Override
    public CallbackMediaPlayerComponent getTrueMediaPlayer() {
        return mediaPlayer;
    }

    @Override
    public MatrixStack render(MatrixStack matrixStack, int x, int y) {
        return matrixStack;
    }

    @Override
    public ResourceLocation renderImage() {
        return super.renderImage();
    }

    @Override
    public void bindFrame() {
        // Void Callback
    }

}

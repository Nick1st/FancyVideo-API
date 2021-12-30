package nick1st.fancyvideo.api;

import uk.co.caprica.vlcj.player.base.MediaPlayer;
import uk.co.caprica.vlcj.player.embedded.videosurface.callback.BufferFormat;
import uk.co.caprica.vlcj.player.embedded.videosurface.callback.RenderCallback;

import java.nio.ByteBuffer;

public class ByteBufferCallback implements RenderCallback {
    private final nick1st.fancyvideo.api.MediaPlayer mediaPlayer;

    public ByteBufferCallback(nick1st.fancyvideo.api.MediaPlayer mediaPlayer) {
        this.mediaPlayer = mediaPlayer;
    }

    public static ByteBuffer cloneByteBuffer(final ByteBuffer original) {
        // Create clone with same capacity as original.
        final ByteBuffer clone = (original.isDirect()) ?
                ByteBuffer.allocateDirect(original.capacity()) :
                ByteBuffer.allocate(original.capacity());

        // Create a read-only copy of the original.
        // This allows reading from the original without modifying it.
        final ByteBuffer readOnlyCopy = original.asReadOnlyBuffer();

        // Flip and read from the original.
        readOnlyCopy.flip();
        clone.put(readOnlyCopy);

        return clone;
    }

    @Override
    public void display(MediaPlayer mediaPlayer, ByteBuffer[] nativeBuffers, BufferFormat bufferFormat) {
        onDisplay(cloneByteBuffer(nativeBuffers[0]));
    }

    protected void onDisplay(ByteBuffer byteBuffer) {
        this.mediaPlayer.setByteBuffer(byteBuffer);
    }
}

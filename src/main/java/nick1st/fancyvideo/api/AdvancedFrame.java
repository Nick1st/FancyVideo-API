package nick1st.fancyvideo.api;

public class AdvancedFrame {
    int[] frame;
    int width;

    public AdvancedFrame(int[] frame, int width) {
        this.frame = frame;
        this.width = width;
    }

    AdvancedFrame(AdvancedFrame toClone) {
        if (toClone.frame == null) {
            this.frame = new int[0];
        } else {
            this.frame = toClone.frame.clone();
        }
        this.width = toClone.width;
    }
}

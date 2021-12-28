import com.mojang.blaze3d.matrix.MatrixStack;
import net.minecraft.client.renderer.Tessellator;
import nick1st.fancyvideo.BufferToMatrixStack;

import java.io.IOException;
import java.math.BigInteger;
import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class SpeedTests {
    public static int[] image;
    public static long ts1;
    public static long ts2;
    public static long[] timestamps = new long[10];

    public static void main(String[] args) {
        try {
            image = BufferToMatrixStack.extractBytes("D:\\Programmieren\\vlcj-test-mod\\run\\image.png");



            for (int i = 0; i < timestamps.length; i++) {
                ts1 = System.nanoTime();
                process1();
                ts2 = System.nanoTime();
                timestamps[i] = ts2 - ts1;
                System.out.println(i);
            }
            BigInteger integer = BigInteger.ZERO;
            for (long timestamp : timestamps) {
                integer = integer.add(BigInteger.valueOf(timestamp));
            }
            System.out.println(integer.divide(BigInteger.valueOf(timestamps.length)));



            for (int i = 0; i < timestamps.length; i++) {
                ts1 = System.nanoTime();
                process2();
                ts2 = System.nanoTime();
                timestamps[i] = ts2 - ts1;
                System.out.println(i);
            }
            integer = BigInteger.ZERO;
            for (long timestamp : timestamps) {
                integer = integer.add(BigInteger.valueOf(timestamp));
            }
            System.out.println(integer.divide(BigInteger.valueOf(timestamps.length)));



        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void process1() {
        List<Integer> frameList = Arrays.stream(image).boxed().collect(Collectors.toList());
        BufferToMatrixStack bufferStack = new BufferToMatrixStack(new MatrixStack(), new Tessellator(2073600));
        IntStream.range(0, frameList.size()).forEach(index -> {
            int y = index / 1920;
            int x = index % 1920;
            bufferStack.set(x, y, frameList.get(index));
        });
        bufferStack.finishDrawingTest();
    }

    public static void process2() {
        int[] image2 = image.clone();
        BufferToMatrixStack bufferStack = new BufferToMatrixStack(new MatrixStack(), new Tessellator(2073600));
        IntStream.range(0, image2.length).forEach(index -> {
            int y = index / 1920;
            int x = index % 1920;
            bufferStack.set(x, y, image2[index]);
        });
        bufferStack.finishDrawingTest();
    }
}

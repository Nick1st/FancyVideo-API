import com.mojang.blaze3d.matrix.MatrixStack;
import net.minecraft.client.renderer.Tessellator;
import nick1st.fancyvideo.BufferToMatrixStack;

import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class SpeedTests {
    public static int[] image;
    public static long ts1;
    public static long ts2;
    public static long[] timestamps = new long[10];

    public static void main(String[] args) {
        colorFlipper();
  /*      try {
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
        }*/
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

    public static void colorFlipper() {
        System.out.println("Preparing Color Flipper");
        ts1 = System.nanoTime();
        Random ran = new Random(8327832184671374183L);
        int[] colors = new int[3456000];
        for (int i = 0; i < colors.length; i++) {
            colors[i] = ran.nextInt();
        }
        ts2 = System.nanoTime();
        System.out.println("Finished after " + (ts2 - ts1) + " Nanos");

        System.out.println("Running Complex Stream Color Flipper");
        ts1 = System.nanoTime();
        int[] streamColors = new int[3456000];
        IntStream.range(0, colors.length).forEach(index -> {
            int b = (colors[index] & 255);
            int g = (colors[index] >> 8 & 255);
            int r = (colors[index] >> 16 & 255);
            int a = (colors[index] >> 24 & 255);
            a |= 0xFF;
            streamColors[index] = (a << 24) + (b << 16) + (g << 8) + (r);
        });
        ts2 = System.nanoTime();
        System.out.println("Finished after " + (ts2 - ts1) + " Nanos");

        System.out.println("Running Advanced Color Flipper");
        ts1 = System.nanoTime();
        int[] advancedColors = new int[3456000];
        IntStream.range(0, colors.length).forEach(index -> {
            int color = colors[index];
            color <<= 8;
            color |= 0xFF;
            color = Integer.reverseBytes(color);
            advancedColors[index] = color;
        });
        ts2 = System.nanoTime();
        System.out.println("Finished after " + (ts2 - ts1) + " Nanos");

        System.out.println("Validate Results: ");
        for (int i = 0; i < 10; i++) {
            System.out.println(streamColors[i] + " | " + advancedColors[i]);
        }
    }
}

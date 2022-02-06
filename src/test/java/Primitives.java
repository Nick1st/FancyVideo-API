import java.util.Arrays;

public class Primitives {

    public static void main(String[] args) {
        int i = 10;
        int[] ia = {1, 2, 3};
        TestObj obj = new TestObj(ia.clone(), i);
        i = 12;
        ia[1] = 4;
        ia[0] = 5;
        ia[2] = 6;
        System.out.println(i);
        System.out.println(Arrays.toString(ia));
        System.out.println(obj);
    }

    public static class TestObj {
        int i;
        int[] ia;

        TestObj(int[] in, int in1) {
            ia = in;
            i = in1;
        }

        @Override
        public String toString() {
            return "TestObj{" +
                    "i=" + i +
                    "ia="  + Arrays.toString(ia) +
                    '}';
        }
    }
}

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import java.math.BigDecimal;
import java.util.Scanner;

public class BitwiseShit {
    public static void main(String[] args) throws Exception {
        String operation;
        Scanner consoleinput = new Scanner(System.in);
        System.out.println("Please enter bitwise operations:");
        while (true) {
            operation = consoleinput.nextLine();
            computeAndPrint(operation);
            //System.out.println();
        }
    }

    public static void computeAndPrint(String operation) throws Exception {
        ScriptEngineManager mgr = new ScriptEngineManager();
        ScriptEngine engine = mgr.getEngineByName("JavaScript");
        String outputString = String.valueOf(engine.eval(operation));
        long outputLong = new BigDecimal(outputString).longValue();
        String outputBinary = Long.toBinaryString(outputLong);
        while (outputBinary.length() < 32) {
            outputBinary = "0" + outputBinary;
        }
        //System.out.println(operation+"="+outputLong);
        System.out.println("        " + outputBinary.substring(outputBinary.length() - 32));
    }
}
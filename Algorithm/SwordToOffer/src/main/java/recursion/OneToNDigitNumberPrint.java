package recursion;

import org.junit.Test;

import javax.swing.plaf.synth.SynthOptionPaneUI;
import java.lang.reflect.Array;
import java.util.Arrays;

/**
 * 打印从1到最大n位的十进制数，比如输入3，打印1，2，3……到999，因为3位的最大值是999
 * 如果使用循环打印就没什么意思了，而且当n很大时，可能会溢出，因此要写出更高效的打印
 */
public class OneToNDigitNumberPrint {

    @Test
    public void test() {
//        numberIncrement(new char[]{'1','9','9'});
//        printByString(2);
        printByRecursion(2);
    }

    /**
     * 使用字符串或数组表示来保证大数不会溢出
     * 先确定字符串位数，之后模拟加法，打印
     */
    public void printByString(int n) {
        if(n < 0) {
            System.out.println("digit cant 0");
        }
        char[] number = new char[n];
        Arrays.fill(number, '0');
        while (numberIncrement(number) == false) {
            print(number);
        }

    }

    private boolean numberIncrement(char[] number) {
        boolean isOverFlow = false;
        int carry = 0;      // 进位
        for (int i = number.length - 1; i >= 0; i--) {
            int digit = number[i] - '0' + carry;
            if (i == number.length - 1)
                digit++;
            // 位数到10才会循环，进位，否则直接退出
            if (digit >= 10) {
                if (i == 0) {
                    isOverFlow = true;
                } else {
                    digit = digit - 10;
                    carry = 1;
                    number[i] = (char) ('0' + digit);
                }
            } else {
                number[i] = (char) ('0' + digit);
                break;
            }
        }
        return isOverFlow;
    }

    private void print(char[] number) {
        boolean isBegin0 = true;
        for (int i = 0; i < number.length; i++) {
            if (isBegin0 == true && number[i] != '0') {
                isBegin0 = false;
            }
            if (isBegin0 == false) {
                System.out.print(number[i]);
            }
        }
        System.out.println();
    }

    /**
     * 另一种使用递归，把数字的每一位都从0-9做全排列，排在前面的0不打印
     */
    public void printByRecursion(int n) {
        if(n < 0) {
            System.out.println("digit cant 0");
        }
        char[] number = new char[n];
        numberIncrement(number, 0);
    }

    private void numberIncrement(char[] number, int digit) {
        if (digit == number.length) {
            print(number);
            return;
        }
        // 数字的每一位都是0-9中的一个
        for (int i = 0; i < 10; i++) {
            number[digit] = (char) (i + '0');
            numberIncrement(number, digit + 1);
        }
    }
}

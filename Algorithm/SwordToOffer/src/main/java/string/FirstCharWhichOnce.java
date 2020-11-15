package string;

import org.junit.Test;

/**
 * 在一个字符串中找到第一个只出现一次的字符，并返回它的位置。字符串只包含 ASCII 码字符。
 * Input: abacc
 * Output: b
 */
public class FirstCharWhichOnce {

    @Test
    public void test() {
        System.out.println(getFirstCharWhichOnce1("abacc"));
    }

    /**
     * 第一反应：利用hashMap，遍历两次，一次存，一次找出值为1的字符即可
     * 第二反应：由于字符只包含ASCII码，因此可以把hashMap换成一个长度为128的字符数组存储即可
     */
    private Character getFirstCharWhichOnce1(String str) {
        int[] charArr = new int[128];
        for (int i = 0; i < str.length(); i++) {
            charArr[str.charAt(i)]++;
        }
        for (int i = 0; i < charArr.length; i++) {
            if (charArr[i] == 1) {
                return (char)i;
            }
        }
        return null;
    }
}

package dp;

import org.junit.Test;

/**
 * 给定一个数字，按照如下规则翻译成字符串：1翻译成 a， 2 翻译成 b... 26 翻译成 z。
 * 一个数字有多种翻译可能，例如 12258 一共有 5 种，
 * 分别是 abbeh，lbeh，aveh，abyh，lyh。实现一个函数，用来计算一个数字有多少种不同的翻译方法。
 */
public class NumberTranslateString {

    @Test
    public void test() {

    }

    /**
     * 分析：https://leetcode-cn.com/problems/decode-ways/solution/javajie-ti-si-lu-xiang-jie-by-zackqf/
     * https://leetcode-cn.com/problems/decode-ways/solution/dong-tai-gui-hua-java-python-by-liweiwei1419/
     * 思路：动态规划，12258，拆分成 122和58，而122有3种解法，[1,2,2]、[12,2]、[1,22]
     * 加入5，有3 + 1种解法，[1,2,2,5]，[12,2,5]，[12,25]，[1,22,5]，
     * 5的加入，可以与前面的2进行合并，因为合并产生了1种解法，不能合并则解法不能+1
     * 即要判断新加入的数字能不能与之前的数字进行合并，如果可以，则解法可+1
     * result[i] 表示的是以 number[i]结尾的前缀子串的解法数
     * 如果number[i] == '0', 说明不能解码， != '0'时，此时 result[i] = result[i - 1]
     * 如果number[i - 1]和number[i]组成 >= 10 , <= 26的数，此时 result[i] += result[i - 2]
     */
    private int countTranslateNum(String number) {
        if (number == null || number.length() <= 0) {
            return 0;
        }
        int[] result = new int[number.length()]; // result是前i个字符可以得到的解码总数
        result[0] = 1;
        for (int i = 1; i < number.length(); i++) {

        }
        return result[number.length() - 1];
    }
}

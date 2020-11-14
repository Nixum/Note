package dp;

import org.junit.Test;

import java.util.Arrays;

/**
 * 输入一个字符串（只包含 a~z 的字符），求其最长不含重复字符的子字符串的长度。
 * 例如对于 arabcacfr，最长不含重复字符的子字符串为 acfr，长度为 4。
 */
public class LongestSubStringWithoutDuplication {

    @Test
    public void test() {
        System.out.println(getLongestSubStringCountWithoutDuplication("arabcacfr"));
    }

    /**
     * 思路：动态规划。遍历字符串，以第i个字符结尾时，最长不重复字符串为f(i)，
     * 如果第i个字符之前没出现过，此时f(i) = f(i-1) + 1
     * 如果第i个字符之前出现过，分两种情景
     * 1.如果第i个字符与最近一个与其重复的字符的距离d 小于或等于f(i-1)，即重复字符已包含在f(i-1)中，
     *   且两个重复字符所夹字符也没有出现重复，此时f(i) = d，即最长距离为以该字符的重复字符后的一个字符到该字符的距离
     * 2.如果第i个字符与最近一个与其重复的字符的距离d 大于f(i-1)，即重复字符已在f(i-1)之前，此时f(i) = f(i-1) + 1
     */
    private int getLongestSubStringCountWithoutDuplication(String str) {
        // 利用字符串所包含的字符在a-z之间的特点，初始化一个长度为26的字符数组，记录重复字符的下标
        int[] charPosition = new int[26];
        Arrays.fill(charPosition, -1);

        char[] charArr = str.toCharArray();
        int maxLen = 0; // 最大长度
        int curLen = 0; // 当前长度
        for (int i = 0; i < charArr.length; i++) {
            int curI = charPosition[charArr[i] - 'a'];
            if (curI < 0 || i - curI > curLen) { // 字符没出现过 或者 重复字符距离大于当前子串长度
                curLen ++;
            } else {
                if (curLen > maxLen) {  // 获取最大值
                    maxLen = curLen;
                }
                curLen = i - curI; // 重复字符包含在子串里，此时的不重复子串长度 = 两重复字符的距离
            }
            charPosition[charArr[i] - 'a'] = i;
        }
        return Math.max(curLen, maxLen);
    }
}

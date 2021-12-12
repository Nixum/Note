package string;

import java.util.HashMap;
import java.util.Map;

/**
 * 给定字符串，找出不含有重复字符的最长子串的长度
 * 输入：abcabcbb
 * 输出：3
 * 输入：bbbbb
 * 输出：1
 * 输入：pwwkew
 * 输出：3
 */
public class LongestSubstringWithoutRepeat {

    public int FindLongestSubstringWithoutRepeat(String s) {
        Map<Character, Integer> win = new HashMap<>();
        int left = 0, right = 0;
        int max = 0;
        while (right < s.length()) {
            char c = s.charAt(right);
            right++;
            win.put(c, win.getOrDefault(c, 0) + 1);
            while (win.get(c) > 1) {
                char d = s.charAt(left);
                left++;
                win.put(d, win.get(d) - 1);
            }
            max = Math.max(max, right - left);
        }
        return max;
    }
}

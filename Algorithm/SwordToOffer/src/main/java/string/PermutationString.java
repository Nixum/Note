package string;

import java.util.HashMap;
import java.util.Map;

/**
 * leetcode 567
 * 给定字符串S1和S2，写一个函数判断S2是否包含S1的排列，即第一个字符串的排列之一是第二个字符串的子串
 * 输入：s1 = "ab"， s2 = "eidbaooo"
 * 输出：True
 *
 * 输入：s1 = "ab"，s2 = "eidboaoo"
 * 输出：False
 */
public class PermutationString {

    public boolean checkInclusion(String t, String s) {
        Map<Character, Integer> need = new HashMap<>();
        Map<Character, Integer> win = new HashMap<>();
        for (int i = 0; i < t.length(); i++) {
            char c = t.charAt(i);
            need.put(c, need.getOrDefault(c, 0) + 1);
        }
        int left = 0, right = 0;
        int valid = 0;
        while (right < s.length()) {
            char c = s.charAt(right);
            right ++;
            if (need.containsKey(c)) {
                win.put(c, win.getOrDefault(c, 0) + 1);
                if (win.get(c).equals(need.get(c))) {
                    valid++;
                }
            }
            // 说明已经符合子串长度了，判断该区间内是否包含子串
            while(right - left >= t.length()) {
                // 说明已经找到符合的子串了
                if (valid == need.size()) {
                    return true;
                }
                char d = s.charAt(left);
                left++;
                if (need.containsKey(d)) {
                    if (win.get(d).equals(need.get(d))) {
                        valid--;
                    }
                    win.put(d, win.get(d) - 1);
                }
            }
        }
        return false;
    }

}

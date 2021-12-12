package string;

import org.junit.Test;

import java.util.*;

/**
 * leetcode 76
 * 给定一个字符串S，一个字符串T，在字符串S里找出：包含T所有字母的最小子串
 * 输入：S = "ADOBECODEBANC", T = "ABC"
 * 输出：BANC
 * 如果S中不存在这样的子串，返回空串
 * 如果S中存在这样的子串，它需要唯一，且最短
 */
public class MinWindowSubstring {

    @Test
    public void test() {
        System.out.println(minWindow("ADOBECODEBANC", "ABC"));
    }

    public String minWindow(String s, String t) {
        int left = 0, right = 0;
        // 填充需要的字符
        Map<Character, Integer> need = new HashMap<>();
        for (int i = 0; i < t.length(); i++) {
            char c = t.charAt(i);
            need.put(c, need.getOrDefault(c, 0) + 1);
        }

        Map<Character, Integer> win = new HashMap<>();
        int start = 0, len = Integer.MAX_VALUE;
        int valid = 0; // 统计字符出现的种类数
        // 开始左右窗口的滑动
        while (right < s.length()) {
            char c = s.charAt(right);
            right++;
            // 一直移动右窗口，直到包含所有目标字符
            if (need.containsKey(c)) {
                win.put(c, win.getOrDefault(c, 0) + 1);
                if (win.get(c).equals(need.get(c))) {
                    valid++;
                }
            }
            // 包含所有目标字符后，开始缩小窗口，
            while (valid == need.size()) {
                // 更新最小覆盖子串的索引
                if (right - left < len) {
                    start = left;
                    len = right - left;
                }
                // 移除左边的字符
                char d = s.charAt(left);
                left++;
                // 判断是否是重复字符，是则移除
                if (need.containsKey(d)) {
                    // 滑动窗口内的字符跟目标字符的个数一致，说明已经是最小子串了，破坏循环条件
                    if (win.get(d).equals(need.get(d))) {
                        valid--;
                    }
                    win.put(d, win.get(d) - 1);
                }
            }
        }
        return len == Integer.MAX_VALUE ? "" : s.substring(start, start + len);
    }

}

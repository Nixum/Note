package string;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** leetcode 438
 * 给定字符串s和非空字符串p，找到s中所有是p的字母异位词的子串，返回这些子串的起始索引
 * 字符串只包含小写英文字母，且字符串s和p的长度都不超过20100
 * 异位词指字母相同，排列不同的字符串
 * 输入：s = "cbaebabacd", p = "abc"
 * 输出：[0, 6]
 * 因为0为起始索引的字符串为cba，6为起始索引的字符串为bac，都是abc的异位词
 */
public class AnagramsFind {

    public int[] findAnagrams(String s, String t) {
        Map<Character, Integer> need = new HashMap<>();
        Map<Character, Integer> win = new HashMap<>();
        for (int i = 0; i < t.length(); i++) {
            char c = t.charAt(i);
            need.put(c, need.getOrDefault(c, 0) + 1);
        }
        int left = 0, right = 0;
        int valid = 0;
        List<Integer> res = new ArrayList<>();
        while (right < s.length()) {
            char c = s.charAt(right);
            right++;
            if (need.containsKey(c)) {
                win.put(c, win.getOrDefault(c, 0) + 1);
                if (win.get(c).equals(need.get(c))) {
                    valid++;
                }
            }
            // 长度大于目标字符串的长度，窗口缩减
            while(right - left >= t.length()) {
                if (valid == need.size()) {
                    res.add(left);
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
        return res.stream().mapToInt(Integer :: intValue).toArray();
    }
}

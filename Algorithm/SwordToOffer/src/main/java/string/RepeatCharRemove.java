package string;

import java.util.Stack;

/**
 * leetcode 316，1081
 * 去除重复字母
 * 给定一个仅包含小写字母的字符串，去除字符串中重复的字母，使得每个字母只出现一次，
 * 且保证返回结果的字典序最小
 * 输入：bcabc，返回：abc
 * 输入：cbacdcbc，返回：acdb
 */
public class RepeatCharRemove {

    /**
     * 利用单调栈 + 两个辅助数组，因为只有Asics码，所以数组长度可以是256
     * 一个用于记录字符是否出现过，一个用于记录字符是否在栈内
     */
    public String remove(String s) {
        int[] count = new int[256];
        Stack<Character> stack = new Stack<>();
        boolean[] isIn = new boolean[256];
        for (int i = 0; i < s.length(); i++) {
            count[s.charAt(i)]++;
        }
        for (char c : s.toCharArray()) {
            count[c]--;
            if (isIn[c]) {
                continue;
            }
            // 如果后加入的字符小于当前栈顶字符
            while (!stack.isEmpty() && stack.peek() > c) {
                // 该字符只出现过一次，则只能入栈
                if (count[stack.peek()] == 0) {
                    break;
                }
                // 否则移除最开始出现的一次
                isIn[stack.pop()] = false;
            }
            stack.push(c);
            isIn[c] = true;
        }
        StringBuilder sb = new StringBuilder();
        while (!stack.isEmpty()) {
            sb.append(stack.pop());
        }
        return sb.reverse().toString();
    }
}

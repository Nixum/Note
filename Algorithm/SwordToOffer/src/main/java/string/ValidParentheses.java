package string;

import org.junit.Test;

import java.util.Stack;

/**
 * leetcode 20
 * 判断是否是合法的括号串
 * 输入：(){}[]，返回true
 * 输入：([)]，返回false
 * 输入：{[]}，返回true
 */
public class ValidParentheses {

    @Test
    public void test() {

    }

    /**
     * 利用栈，记录括号是否配对
     */
    public boolean isValid(String s) {
        Stack<Character> stack = new Stack<>();
        for (char c : s.toCharArray()) {
            if (c == '(' || c == '{' || c == '[') {
                stack.push(c);
            } else {
                if (!stack.isEmpty() && stack.peek() == parenthesesLeftOf(c)) {
                    stack.pop();
                } else {
                    return false;
                }
            }
        }
        return stack.empty();
    }

    Character parenthesesLeftOf(Character c) {
        if (c == '}') {
            return '{';
        } else if (c == ')') {
            return '(';
        } else {
            return '[';
        }
    }
}

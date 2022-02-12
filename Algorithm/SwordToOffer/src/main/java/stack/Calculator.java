package stack;

import org.junit.Test;
import sun.security.util.Length;

import java.util.Stack;

/**
 * leetcode 224、227、772
 * 实现加、减、乘、除计算器
 */
public class Calculator {

    @Test
    public void test() {
        System.out.println(calculate("3*(4-5/2+(-5/2+4))-6"));
    }

    int a = 0;
    public int calculate(String s) {
        Stack<Integer> stack = new Stack<>();
        int num = 0;
        char sign = '+';
        for (int i = 0; i < s.length(); i = i + a + 1) {
            a = 0;
            char c = s.charAt(i);
            if (isDigit(c)) {
                num = 10 * num + (c - '0');
            }
            if (c == '(') {
                num = calculate(s.substring(i + 1));
            }
            if (!isDigit(c) && c != ' ' || i == s.length() - 1) {
                int pre = 0;
                switch (sign) {
                    case '+':
                        stack.push(num);
                        break;
                    case '-':
                        stack.push(-num);
                        break;
                    case '*':
                        pre = stack.peek();
                        stack.pop();
                        stack.push(pre * num);
                        break;
                    case '/':
                        pre = stack.peek();
                        stack.pop();
                        stack.push(pre / num);
                        break;
                }
                sign = c;
                num = 0;
            }
            if (c == ')') {
                a = i + 1;
                break;
            }
        }
        int res = 0;
        while (!stack.empty()) {
            res += stack.peek();
            stack.pop();
        }
        return res;
    }

    private boolean isDigit(char c) {
        return c >= '0' && c <= '9';
    }
}

package string;

/**
 * leetcode 921
 * 给你输⼊⼀个字符串 s，你可以在其中的任意位置插⼊左括号 ( 或者右括号 )，请问你最少需要⼏次插⼊才
 * 能使得 s 变成⼀个合法的括号串？
 * ⽐如说输⼊ s = "())("，算法应该返回 2，因为我们⾄少需要插⼊两次把 s 变成 "(())()"，这样每个左
 * 括号都有⼀个右括号匹配，s 是⼀个合法的括号串
 */
public class BalanceParentheses1 {

    public int minAddToMakeValid(String s) {
        // 需要插入左括号的次数
        int needLeft = 0;
        // 对右括号的需求
        int needRight = 0;
        for (Character c : s.toCharArray()) {
            // 对右括号需求+1
            if (c == '(') {
                needRight++;
            }
            // 对右括号需求-1
            if (c == ')') {
                needRight--;
                // 需要一个左括号
                if (needRight == -1) {
                    needRight = 0;
                    needLeft++;
                }
            }
        }
        return needLeft + needRight;
    }
}

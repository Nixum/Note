package string;

/**
 * leetcode 1541
 * 假设 1 个左括号需要匹配 2 个右括号才叫做合法的括号组合，那么给你输⼊⼀个括号串 s，请问你如何
 * 计算使得 s 合法的最⼩插⼊次数呢
 * 比如："())", "())(())))" and "(())())))" 是平衡的, ")()", "()))" and "(()))" 则不平衡
 * 输入：(()))，输出：1
 * 输入：())，输出：0
 * 输入：))())(，输出：3
 */
public class BalanceParentheses2 {

    public int minInsertions(String s) {
        // 修改次数
        int res = 0;
        // 对右括号的需求
        int needRight = 0;
        for (Character c : s.toCharArray()) {
            // 对右括号需求+1
            if (c == '(') {
                needRight += 2;
                // 当此时是左括号，但是右括号的个数为奇数时，需要插入一个右括号
                if (needRight % 2 == 1) {
                    res++;
                    needRight--;
                }
            }
            // 对右括号需求-1
            if (c == ')') {
                needRight--;
                // 需要一个左括号
                if (needRight == -1) {
                    needRight = 1;
                    res++;
                }
            }
        }
        return res + needRight;
    }
}

package string;

/**
 * leetcode 8
 * 数字字符串转整型
 * 读入字符串并丢弃无用的前导空格
 * 检查下一个字符（假设还未到字符末尾）为正还是负号，读取该字符（如果有）。
 * 确定最终结果是负数还是正数。 如果两者都不存在，则假定结果为正。
 * 读入下一个字符，直到到达下一个非数字字符或到达输入的结尾。字符串的其余部分将被忽略。
 * 将前面步骤读入的这些数字转换为整数（即，"123" -> 123， "0032" -> 32）。
 * 如果没有读入数字，则整数为 0 。必要时更改符号（从步骤 2 开始）。
 * 如果整数数超过 32 位有符号整数范围 [−231, 231− 1] ，
 * 需要截断这个整数，使其保持在这个范围内。具体来说，小于 −231 的整数应该被固定为 −231 ，大于 231− 1 的整数应该被固定为 231− 1 。
 * 返回整数作为最终结果。
 *
 * 比如
 * 输入 "42"，输出 42
 * 输入 "   -42"，输出 -42
 * 输入 "4193 with words"，输出 4193
 * 输入 "words and 987"， 输出 0
 * 输入 "-91283472332"， 输出 -2147483648，因为-91283472332小于 -2^31，所以直接-2^31=-2147483648
 * 数组长度 [0, 200]
 * 字符串只能包含 0-9a-zA-Z 空格 + - .
 */
public class ATOI {

    /**
     * 关于如何判断整型溢出： num > Integer.MAX_VALUE / 10 即可
     */
    public int myAtoi(String s) {
        int len = s.length();
        char[] charArray = s.toCharArray();

        // 去除前导空格
        int index = 0;
        while(index < len && charArray[index] == ' ') {
            index++;
        }
        // 判断是否全是空格
        if (index == len) {
            return 0;
        }
        // 判断 + -
        int sign = 1;
        char firstChar = charArray[index];
        if (firstChar == '+') {
            index++;
        } else if (firstChar == '-') {
            index++;
            sign = -1;
        }
        int res = 0;
        while (index < len) {
            char currChar = charArray[index];
            // 判断非法整型字符串
            if (currChar > '9' || currChar < '0') {
                break;
            }
            // 判断溢出，直接溢出 或者 当位数一样时，加上当前数字就会溢出
            if (res > Integer.MAX_VALUE / 10 || (res == Integer.MAX_VALUE / 10 && (currChar - '0') > Integer.MAX_VALUE % 10)) {
                return Integer.MAX_VALUE;
            }
            if (res < Integer.MIN_VALUE / 10 || (res == Integer.MIN_VALUE / 10 && (currChar - '0') > - (Integer.MIN_VALUE % 10))) {
                return Integer.MIN_VALUE;
            }
            // 转化计算
            res = res * 10 + sign * (currChar - '0');
            index++;
        }
        return res;
    }
}

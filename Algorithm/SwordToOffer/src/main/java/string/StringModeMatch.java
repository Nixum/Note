package string;

import org.junit.Test;

/**
 * 正则表达式匹配，匹配是指字符串的所有字符匹配整个模式
 * '.'表示任意一个字符，'*'表示前一个字符可以出现0-n次
 * 例如：字符串aaa，匹配 a.a、ab*ac*a
 *      不匹配 aa.a、ab*a
 */
public class StringModeMatch {

    @Test
    public void test() {
        boolean flag = isMatch("aaa".toCharArray(), "ab*ac*a".toCharArray());
        System.out.println(flag);
    }

    /**
     * 比较两个字符串的每一个字符，从第一个字符开始，如果第一个字符就不相同直接返回false
     * 遇到'.'，两个字符串上指针直接选择下一个字符
     * 遇到'*'，分两种情况，1.模式串上向后移动两个字符 2.保存模式不变
     */
    public boolean isMatch(char[] str, char[] pattern) {
        if (str == null || pattern == null)
            return false;

        return match(str, 0, pattern, 0);
    }

    private boolean match(char[] str, int i, char[] pattern, int j){
        if (i == str.length - 1 && j == pattern.length - 1)
            return true;
        if (i >= str.length || j >= pattern.length || j + 1 >= pattern.length)
            return false;

        // 在下一位是'*'的情况下
        if (pattern[j + 1] == '*') {
            // 当前两个字符是否相等，如果相等，分情况
            if (str[i] == pattern[j] || (pattern[j] == '.' && i != str.length - 1)) {
                return match(str, i + 1, pattern, j + 2) ||     //匹配*是1个的情况
                        match(str, i + 1, pattern, j) ||          //匹配*是多个的情况
                        match(str, i, pattern, j + 2);            //匹配*是0个的情况
            } else {    // 如果不等，*直接略过，比较模式里*后面的字符
                return match(str, i, pattern, j + 2);   //
            }
        }
        // 遇到'.'直接比较下一位
        if (str[i] == pattern[j] || (pattern[j] == '.' && i != str.length - 1)) {
            return match(str, i + 1, pattern, j + 1);
        }
        return false;
    }

}

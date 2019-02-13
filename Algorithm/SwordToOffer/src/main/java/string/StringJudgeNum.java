package string;

import org.junit.Test;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 判断字符串是否是数字
 * 例子：
 * +100、5e2、-123、3.1416、-1E-16 是数字
 *
 * 12e、1a3.14、1.2.3、+-5、12e+4.3 不是数字
 */
public class StringJudgeNum {

    @Test
    public void test() {
        System.out.println(isNumberic("+100"));
        System.out.println(isNumberic("5e2"));
        System.out.println(isNumberic("-123"));
        System.out.println(isNumberic("3.1416"));

        System.out.println(isNumberic("12e"));
        System.out.println(isNumberic("1.2.3"));
        System.out.println(isNumberic("1a3"));
        System.out.println(isNumberic("12e+4.3"));
    }

    /**
     * 直接正则表达式解决
     * + : 1-n个     ? : 0-1个
     * * ：0-n个     . : 任意字符
     * \\d ： 数字     \\D : 非数字
     */
    public boolean isNumberic(String str) {
        if (str == null || str.length() == 0 || "".equals(str.trim()))
            return false;
        Pattern p = Pattern.compile("[\\+-]?\\d*(\\.\\d+)?([eE][\\+-]?\\d+)?");
        Matcher matcher = p.matcher(str);
        return matcher.matches();
    }
}

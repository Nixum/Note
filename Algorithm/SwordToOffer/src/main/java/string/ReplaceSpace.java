package string;

import org.junit.Test;

import java.net.StandardSocketOptions;
import java.util.Arrays;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 把字符串每个空格替换成%20
 */
public class ReplaceSpace {

    @Test
    public void test() {
        String s = "We Are Happy";
        String result = replaceSpaceByApi(s);
        System.out.println(result);

        char[] c = replaceSpace(s.toCharArray());
        System.out.println(c);
    }

    public String replaceSpaceByApi(String target) {
//        target = target.replace(" ","%20");
        // 本质是正则表达式
        String regax = " ";
        Pattern pattern = Pattern.compile(regax);
        Matcher matcher = pattern.matcher(target);
        StringBuffer sb = new StringBuffer();
        while (matcher.find()) {
            matcher.appendReplacement(sb,"%20");
        }
        matcher.appendTail(sb);
        return sb.toString();
    }

    /*
     * 在原字符串的基础上进行替换，使用两个指针从后往前遍历，p1指向字符串末端，p2指向替换后的字符串末端，遍历替换
     * 从前往后复制会导致后面的字符移动次数增多，因此选择从后往前复制减少移动次数
     */
    public char[] replaceSpace(char[] target) {
        if (target == null || target.length < 0)
            return null;
        int oldLength = target.length;
        int newLength = oldLength;
        for (int i = 0; i < target.length; i++) {
            if (target[i] == ' ') {
                newLength += 2;
            }
        }
        //扩容,这里引用指向新数组,因为java是值传递，所以外面的字符数组没改变，只能返回回去
        target = Arrays.copyOf(target, newLength);
        int p1 = oldLength - 1;
        int p2 = newLength - 1;
        while (p1 >= 0) {
            if (target[p1] == ' ') {
                target[p2] = '0';
                target[p2 - 1] = '2';
                target[p2 - 2] = '%';
                p2 -= 3;
            } else {
                target[p2] = target[p1];
                p2--;
            }
            p1--;
        }
        return target;
    }
}

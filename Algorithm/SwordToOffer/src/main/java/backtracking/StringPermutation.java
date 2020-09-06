package backtracking;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * 输入一个字符串，按字典序打印出该字符串中字符的所有排列。
 * 例如输入字符串 abc，则打印出由字符 a, b, c
 * 所能排列出来的所有字符串 abc, acb, bac, bca, cab 和 cba。
 */
public class StringPermutation {

    @Test
    public void test() {
        List<String> result = permutation("abc");
        System.out.println(result);
    }

    /**
     * 实际上就是全排列，
     * 第一个字符与第一个字符交换，对后面的字符做全排列（递归），交换回来
     * 第一个字符与第二个字符交换，对后面的字符做全排列（递归），交换回来
     *    .
     *    .
     *    .
     * 第一个字符与第n个字符交换，对后面的字符做全排列（递归），交换回来
     *
     * 归纳起来就是第一步，将第一个字符与后面的所有字符交换
     * 第二步，固定第一个字符，对后面的字符做全排列（重复这两步），
     * 每交换一次，就全排列一次，之后换回来，第一个字符与第二个字符交换，再全排列一次，
     * 保证每个字符都出现在第一个一次
     */
    public List<String> permutation(String str) {
        if(str == null || str.length() <= 0 || "".equals(str.trim()))
            return null;
        List<String> resultList = new ArrayList<>();
        permutation(0, str.toCharArray(), resultList);
        return resultList;
    }

    // index为固定字符的下标
    private void permutation(int index,char[] str, List<String> resultList) {
        if (index == str.length - 1) {
            resultList.add(String.valueOf(str));
        } else {
            for (int i = index; i < str.length; i++) {
                // 固定下标为index的字符，与它之后的每个字符进行交换
                char temp = str[index];
                str[index] = str[i];
                str[i] = temp;

                permutation(index + 1, str, resultList);

                // 换回来
                temp = str[index];
                str[index] = str[i];
                str[i] = temp;
            }
        }
    }
}

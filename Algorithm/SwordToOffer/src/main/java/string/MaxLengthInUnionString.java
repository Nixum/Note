package string;

import java.util.ArrayList;
import java.util.List;

/**
 * leetcode 1239
 * 给定一个字符串数组 arr，字符串 s 是将 arr 某一子序列字符串连接所得的字符串，
 * 如果 s 中的每一个字符都只出现过一次，那么它就是一个可行解。
 * 请返回所有可行解 s 中最长长度。
 *
 * 输入：arr = ["un","iq","ue"]
 * 输出：4
 * 解释：所有可能的串联组合是 "","un","iq","ue","uniq" 和 "ique"，最大长度为 4。
 *
 * 输入：arr = ["cha","r","act","ers"]
 * 输出：6
 * 解释：可能的解答有 "chaers" 和 "acters"。
 *
 * 输入：arr = ["abcdefghijklmnopqrstuvwxyz"]
 * 输出：26
 */
public class MaxLengthInUnionString {

    /**
     * 字符串中只含有小写字母，
     * 故可以使用26位二进制数来表示字符串，含该字母则为1不含则为0.
     * 这样，字符串的拼凑可以使用 或运算，判断是否含重复字符串可以使用 与运算，
     * 而且二进制位也便于统计字符的数量。
     */
    int res = 0;
    public int maxLength(List<String> arr) {
        //先对字符串进行预处理，避免直接有重复字符的字符串
        List<Integer> list = new ArrayList<>();
        for(String s : arr){
            int mask = 0;
            for (int i = 0; i < s.length(); i++) {
                int ch = s.charAt(i) - 'a';
                //判断在该字符串中有没有相同字符出现
                if (((mask >> ch) & 1) != 0) {
                    mask = 0;
                    break;
                }
                // 顺便转成二进制数
                mask = mask | (1 << ch);
            }
            //把代表该字符串的二进制数添加进来
            if (mask > 0) {
                list.add(mask);
            }
        }
        dfs(list,0,0);
        return res;
    }

    private void dfs(List<Integer> arr, int pos, int mask){
        if (pos == arr.size()) {
            res = Math.max(res, Integer.bitCount(mask));
            return;
        }
        //如果字符串数组中的这个字符串与已经连接好的字符串没有相同字符，就可以接着dfs
        if ((mask & arr.get(pos)) == 0) {
            dfs(arr,pos + 1,mask | arr.get(pos));
        }
        //若有相同字符，则不取该字符
        dfs(arr,pos + 1, mask);
    }

}

package array;

import org.junit.Test;

import java.util.Arrays;

/**
 * 输入一个正整数数组，把数组里所有数字拼接起来排成一个数，
 * 打印能拼接出的所有数字中最小的一个。例如输入数组 {3，32，321}，则打印出这三个数字能排成的最小数字为 321323。
 */
public class CombineNumArray {

    @Test
    public void test() {
        System.out.println(getMinCombineNumber(new int[]{1, 323, 21}));
    }

    /**
     * 思路：本质是排序问题，数组中的数组两两组合进行比较，找出组合最小的两个，判断是否要交换
     * 比如，比较两个字符串S1和S2的大小时，比较S1 + S2和S2 + S1的大小，如果大于，说明组合后，S2要排在S1前面
     */
    private String getMinCombineNumber(int[] arr) {
        if (arr == null || arr.length <= 0) {
            return "";
        }
        String[] combineNums = new String[arr.length];
        // 把数字数组转为字符串数组
        for (int i = 0; i < arr.length; i++) {
            combineNums[i] = arr[i] + "";
        }
        for (int i = 0; i < combineNums.length; i++) {
            for (int j = i + 1; j < combineNums.length; j++) {
                if ((combineNums[i] + combineNums[j]).compareTo(combineNums[j] + combineNums[i]) > 0) {
                    String tmp = combineNums[i];
                    combineNums[i] = combineNums[j];
                    combineNums[j] = tmp;
                }
            }
        }
        String result = "";
        for (String str : combineNums) {
            result += str;
        }
        return result;
    }
}

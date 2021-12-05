package array;

import org.junit.Test;

import java.util.Arrays;

/** leetcode 370，类似的题目还有 1109, 1094
 * 有一个长度为n的数组，初始情况下所有数字均为0，然后会给出k个操作，
 * 其中每个操作会被表示为一个三元组 [startIndex, endIndex, inc]，你需要将子数组 A[startIndex ... endIndex]
 * 包括startIndex和endIndex，增加inc
 * 返回k次操作后的数组
 * 输入：length = 5，updates = [[1,3,2], [2,4,3],[0,2,-2]]
 * 输出：[-2, 0, 3, 5, 3]
 * 过程：
 * 初始状态为 [0,0,0,0,0]
 * 进行[1,3,2]操作后变成了[0,2,2,2,0]
 * 进行[2,4,3]操作后变成了[0,2,5,5,3]
 * 进行[0,2,-2]操作后变成了[-2,0,3,5,3]
 */
public class RangeAddition {

    @Test
    public void test() {
        int[][] updates = new int[][]{
                {1,3,2},
                {2,4,3},
                {0,2,-2}
        };
        System.out.println(Arrays.toString(getModifiedArray(5, updates)));
    }

    /**
     * 前缀和是 preSum[i] = nums[i - 1] + nums[i]，可以通过preSum前缀和数组来还原出原数组
     * 差分数组是 preDiff[i] = nums[i] - nums[i - 1]，也可以还原出原数组
     * 对指定区间[i, j]进行加操作，相当于在差分数组上，preDiff[i]上加，在preDiff[j+1]上减，
     * 区间[i,j]因为都是加，对应的差分数是不变的，对数组进行一系列操作之后，再把它还原即可
     */
    public int[] getModifiedArray(int len, int[][] updates) {
        int[] res = new int[len];
        // 构造差分数组, 题目中初始数组值为0，对应的差分数组也全为0
        int[] preDiff = new int[len];

        // 遍历二维数组，对差分数组进行操作
        for (int i = 0; i < updates.length; i++) {
            if (updates[i].length != 3) {
                return res;
            }
            int endI = updates[i][1];
            int startI = updates[i][0];
            if (endI == len || startI < 0) {
                return res;
            }
            int inc = updates[i][2];
            preDiff[startI] += inc;
            if (endI + 1 < len) {
                preDiff[endI + 1] -= inc;
            }
        }
        // 还原数组
        res[0] = preDiff[0];
        for (int i = 1; i < len; i++) {
            res[i] = preDiff[i] + res[i - 1];
        }
        return res;
    }
}

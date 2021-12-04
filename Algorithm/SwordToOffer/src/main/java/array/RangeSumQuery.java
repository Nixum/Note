package array;


import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

/** leetcode 303
 * 给定一个整数数组nums，求出数组从索引 i 到 j (i <= j)范围内元素的总和，包含 i、j 两点
 * Input
 * ["NumArray", "sumRange", "sumRange", "sumRange"]
 * [[[-2, 0, 3, -5, 2, -1]], [0, 2], [2, 5], [0, 5]]
 * Output
 * [null, 1, -1, -3]
 *
 * Explanation
 * NumArray numArray = new NumArray([-2, 0, 3, -5, 2, -1]);
 * numArray.sumRange(0, 2); // return (-2) + 0 + 3 = 1
 * numArray.sumRange(2, 5); // return 3 + (-5) + 2 + (-1) = -1
 * numArray.sumRange(0, 5); // return (-2) + 0 + 3 + (-5) + 2 + (-1) = -3
 */
public class RangeSumQuery {

    /**
     * 一般思路是直接遍历，按left和right的下标进行累加，但是这样每次调用方法的时候都要进循环，
     * 时间复杂度是 O(n)，本质是累加，可以使用前缀和技巧，把遍历过后的值进行累加并存起来，
     * 就直接根据下标即可得到对应的累加值，最后相减即可。
     */
    private int[] preNumsSum;

    public RangeSumQuery(int[] nums) {
        int len = 0;
        if (nums != null) {
            len = nums.length;
        }
        this.preNumsSum = new int[len + 1];
        // 对nums数组进行累加
        for (int i = 1; i < preNumsSum.length; i++) {
            this.preNumsSum[i] = preNumsSum[i - 1] + nums[i - 1];
        }
    }

    public int sumRange(int left, int right) {
        if (left > right || preNumsSum == null || preNumsSum.length <= 0) {
            return 0;
        }
        // 指定下标区间的和，其实就是两个累加数相减
        return this.preNumsSum[right + 1] - this.preNumsSum[left];
    }

    public static void main(String[] args) {
        RangeSumQuery cal = new RangeSumQuery(new int[]{-2, 0, 3, -5, 2, -1});
        System.out.println(cal.sumRange(2, 5));
    }
}

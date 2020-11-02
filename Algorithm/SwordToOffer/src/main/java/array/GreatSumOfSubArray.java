package array;

import org.junit.Test;

/**
 * 在给定数组中连续子数组的最大和， 比如
 * {6, -3, -2, 7, -15, 1, 2, 2}，连续子数组的最大和为 8（从第 0 个开始，到第 3 个为止）
 */
public class GreatSumOfSubArray {

    @Test
    public void test() {
        int[] nums = new int[]{6, -3, -2, 7, -15, 1, 2, 2};
        System.out.println(findGreatSumOfSubArray1(nums));
        int[] nums2 = new int[]{1, -2, 3, 10, -4, 7, 2, -5};
        System.out.println(findGreatSumOfSubArray(nums2));
    }

    /**
     * 暴力解法是算出每种组合，时间复杂度为 n*(n+1)/2, 即O(n^2)
     */
    private int findGreatSumOfSubArray1(int[] nums) {
        if (nums == null || nums.length == 0) {
            return 0;
        }
        int max = Integer.MIN_VALUE;
        for (int i = 0; i < nums.length; i++) {
            int tmpMax = nums[i];
            for (int j = i + 1; j < nums.length; j++) {
                tmpMax += nums[j];
                max = Math.max(tmpMax, max);
            }
        }
        return max;
    }

    /**
     * 思路：观察累加过程，如果前i-1个元素的累加值小于0, 如果加上第i个元素, 得到的结果比第i个元素本身小,
     * 此时, 第i个数字结尾的子数组就是第i个数字本身（负数的最大值就是接近0的数，如果加上第i个还是比i小，就说之前的太小了）。
     * 如果以第i-1个元素的累加值大于0, 则与第i个元素累加就得到以第i个元素结尾的子数组的所有数字和。
     */
    private int findGreatSumOfSubArray(int[] nums) {
        if (nums == null || nums.length == 0) {
            return 0;
        }
        int greatSum = Integer.MIN_VALUE;
        int sum = 0;
        for (int n : nums) {
            sum = sum <= 0 ? n : sum + n;
            greatSum = Math.max(greatSum, sum);
        }
        return greatSum;
    }
}

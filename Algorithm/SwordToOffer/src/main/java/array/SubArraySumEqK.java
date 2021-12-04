package array;

import org.junit.Test;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 * 一个整数数组nums和一个整数k，统计并返回该数组中和为k的 连续子数组 的个数
 * 输入 nums = [1, 1, 1], k=2, 返回 2
 * 输入 nums = [1, 2, 3], k=3, 返回 2
 */
public class SubArraySumEqK {

    @Test
    public void test() {
        System.out.println(subarraySum1(new int[]{1, 1, 1}, 2));
        System.out.println(subarraySum2(new int[]{1, 1, 1}, 2));
    }

    /**
     * 第一种方法：遍历数组，计算每个元素的前缀和，遍历前缀和数组，计算每个连续子数组的和是否等于k
     * 时间复杂度为 O(n*m)
     */
    public int subarraySum1(int[] nums, int k) {
        if (nums == null || nums.length <= 0) {
            return 0;
        }
        // 计算前缀和
        int[] preSum = new int[nums.length + 1];
        for (int i = 0; i < nums.length; i++) {
            preSum[i + 1] = preSum[i] + nums[i];
        }
        System.out.println(Arrays.toString(preSum));
        // 以每一个前缀和，计算每个连续子数组的和是否等于k
        int res = 0;
        for (int i = 1; i < preSum.length; i++) {
            for (int j = 0; j < i; j++) {
                if (k == preSum[i] - preSum[j]) {
                    res++;
                }
            }
        }
        return res;
    }

    /**
     * 第二种方法，优化第一种方案中的嵌套for循环，
     * 优化的关键是将 k == preSum[i] - preSum[j]转换为 preSum[j] == preSum[i] - k
     * 利用map记录preSum[j]出现的次数
     * 时间复杂度为 O(n)
     */
    public int subarraySum2(int[] nums, int k) {
        if (nums == null || nums.length <= 0) {
            return 0;
        }
        int res = 0;
        Map<Integer, Integer> preSum = new HashMap<>();
        preSum.put(0, 1);
        int sumI = 0;
        for (int i = 0; i < nums.length; i++) {
            sumI += nums[i];
            int diff = sumI - k;
            if (preSum.containsKey(diff)) {
                res += preSum.get(diff);
            }
            // 计算前缀和nums[0~i]出现的次数
            preSum.put(sumI, preSum.getOrDefault(sumI, 0) + 1);
        }
        return res;
    }
}

package dp;

import org.junit.Test;

/**
 * 完全背包
 * 给定数组，下标表示第几件物品，值表示该物品的价值，给定指定价值，问有多少种方案装物品
 */
public class FullBackpack {

    @Test
    public void test() {
        System.out.println(all(new int[]{1, 2, 5}, 2));
    }

    /**
     * 动态规划，理解
     * result[j] = result[j] + result[j-value[i]]
     * result[j]表示装前j件物品的选择方案
     */
    public int all(int[] value, int sum) {
        int[] result = new int[sum + 1];
        result[0] = 1;
        for (int i = 0; i < value.length; i++) {
            for (int j = value[i]; j <= sum; j++) {
                result[j] += result[j-value[i]];
            }
        }
        return result[sum];
    }
}

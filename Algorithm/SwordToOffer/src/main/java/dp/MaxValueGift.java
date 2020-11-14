package dp;

import org.junit.Test;

/**
 * 在一个 m*n 的棋盘的每一个格都放有一个礼物，每个礼物都有一定价值（大于 0）。
 * 从左上角开始拿礼物，每次向右或向下移动一格，直到右下角结束。给定一个棋盘，求拿到礼物的最大价值。
 * 例如，对于如下棋盘
 * 1    10   3    8
 * 12   2    9    6
 * 5    7    4    11
 * 3    7    16   5
 * 礼物的最大价值为 1+12+5+7+7+16+5=53
 */
public class MaxValueGift {

    @Test
    public void test() {
        int[][] gift = new int[][] {
            {1,    10,   3,    8},
            {12,   2,    9,    6},
            {5,    7,    4,    11},
            {3,    7,    16,   5}
        };
        System.out.println(getGiftMaxValue(gift));
        System.out.println(getGiftMaxValue2(gift));
        System.out.println(getGiftMaxValue3(gift));
    }

    /**
     * 动态规划：由于每次可以向右或向下移动一格，
     * 对于每一个格子，当前最大值 = 当前值 + max（从左边移动到当前值的累加最大值，从上面移到到当前值的累加最大值）
     * 当遍历走到二维数组右下角时，得到结果。
     */
    private int getGiftMaxValue2(int[][] values) {
        if (values == null || values.length <= 0 || values[0].length <= 0) {
            return 0;
        }
        int n = values.length;
        int m = values[0].length;
        int[][] result = new int[n][m];
        result[0][0] = values[0][0];
        for (int i = 1; i < n; i++) {
            result[i][0] = result[i - 1][0] + values[i][0];
        }
        for (int j = 1; j < m; j++) {
            result[0][j] = result[0][j - 1] + values[0][j];
        }
        for (int i = 1; i < n; i++) {
            for (int j = 1; j < m; j++) {
                result[i][j] = values[i][j] + Math.max(result[i][j - 1], result[i - 1][j]);
            }
        }
        return result[n - 1][m - 1];
    }

    /**
     * getGiftMaxValue2的改良版
     */
    private int getGiftMaxValue3(int[][] values) {
        if (values == null || values.length <= 0 || values[0].length <= 0) {
            return 0;
        }
        int n = values.length;
        int m = values[0].length;
        int[][] result = new int[n][m];
        for (int i = 0; i < n; i++) {
            for (int j = 0; j < m; j++) {
                if (i == 0 && j == 0) {
                    result[i][j] = values[i][j];
                } else if (j == 0) {
                    result[i][j] = result[i - 1][j] + values[i][j];
                } else if (i == 0) {
                    result[i][j] = result[i][j - 1] + values[i][j];
                } else {
                    result[i][j] = values[i][j] + Math.max(result[i][j - 1], result[i - 1][j]);
                }
            }
        }
        return result[n - 1][m - 1];
    }

    /**
     * getGiftMaxValue3的改良版，因为无需把走的每一步都记录下来，只需要留住最大的result即可
     */
    private int getGiftMaxValue(int[][] values) {
        if (values == null || values.length <= 0 || values[0].length <= 0) {
            return 0;
        }
        int n = values.length;
        int m = values[0].length;
        int[] result = new int[n];
        for (int i = 0; i < n; i++) {
            result[0] += values[i][0];
            for (int j = 1; j < m; j++) {
                result[j] = values[i][j] + Math.max(result[j], result[j - 1]);
            }
        }
        return result[n - 1];
    }
}

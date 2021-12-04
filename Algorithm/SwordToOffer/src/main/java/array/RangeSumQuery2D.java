package array;

/**
 * leetcode 304
 * 给定一个二维矩阵matrix，以下类型的多个请求：
 * 计算其子矩阵范围内元素的总和，该子矩阵的左上角为（row1，col1），右下角为（row2，col2）
 * 返回左上角到左下角子矩阵的元素总和
 * 如有矩阵：
 * 3，0，1，4，2
 * 5，6，3，2，1
 * 1，2+，0-，1-，5
 * 4，1-，0-，1-，7
 * 1，0-，3-，0+，5
 * row1，col1为 2，1；row2，col2为4，3，即上图-+的位置，返回8
 */
public class RangeSumQuery2D {

    private int[][] preSum;

    /**
     * 一般思路是直接遍历，从row1，col1到row2，col2的顺序进行累加，但是这样每次调用方法的时候都要进循环，
     * 时间复杂度是 O(n * m)，本质是累加，可以使用前缀和技巧
     * 以（0，0）为起点，当前点（i，j）为终点，算出的该矩形的元素和 =
     * sum(i-1，j) + sum(i，j-1) - sum(i-1，j-1) + (i，j)
     * 以（i1，j1）为起点，（i2，j2）为终点的矩形的元素和 =
     * sum(i2，j2) - sum(i2，j1-1) - sum(i1-1，j2) + sum(i1-1，j1-1)
     */
    public RangeSumQuery2D(int[][] matrix) {
        int m = matrix.length;
        int n = m == 0 ? 0 : matrix[0].length;
        if (m == 0 || n == 0) {
            return;
        }
        // 计算以（0，0）为起点（i，j）为终点的矩形的元素之和
        preSum = new int[m + 1][n + 1];
        for (int i = 1; i <= m; i++) {
            for (int j = 1; j <= n; j++) {
                preSum[i][j] = preSum[i - 1][j] + preSum[i][j - 1] - preSum[i - 1][j - 1] + matrix[i - 1][j - 1];
            }
        }
    }

    public int sumRegion(int row1, int col1, int row2, int col2) {
        return preSum[row2 + 1][col2 + 1] - preSum[row2 + 1][col1] - preSum[row1][col2 + 1] + preSum[row1][col1];
    }

    public void print() {
        for (int i = 0; i < preSum.length; i++) {
            for (int j = 0; j < preSum[i].length; j++) {
                System.out.print(preSum[i][j] + " ");
            }
            System.out.println();
        }
    }

    public static void main(String[] args) {
        int[][] matrix = new int[][]{
                {3, 0, 1, 4, 2},
                {5, 6, 3, 2, 1},
                {1, 2, 0, 1, 5},
                {4, 1, 0, 1, 7},
                {1, 0, 3, 0, 5}
        };
        RangeSumQuery2D s = new RangeSumQuery2D(matrix);
        s.print();
        System.out.println();
        System.out.println(s.sumRegion(2, 1, 4, 3));
        System.out.println(s.sumRegion(1, 1, 2, 2));
        System.out.println(s.sumRegion(1, 2, 2, 4));
    }
}
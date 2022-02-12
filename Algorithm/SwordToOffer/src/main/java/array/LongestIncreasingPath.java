package array;

/**
 * leetocde 329
 * 给定一个m x n 整数矩阵matrix ，找出其中 最长递增路径 的长度。
 * 对于每个单元格，你可以往上，下，左，右四个方向移动。
 * 你不能在对角线方向上移动或移动到边界外（即不允许环绕）
 *
 * 输入：matrix = [[9,9,4],[6,6,8],[2,1,1]]
 * [9*,9,4]
 * [6*,6,8]
 * [2*,1*,1]
 * 输出4，最长递增路径为[1,2,6,9]
 *
 * 输入：matrix = [[3,4,5],[3,2,6],[2,2,1]]
 * [3*,4*,5*]
 * [3,2,6*]
 * [2,2,1]
 * 输出4，最长递增路径为[3, 4, 5, 6]
 *
 * 输入：matrix = [[1]]
 * 输出：1
 */
public class LongestIncreasingPath {

    /**
     * 思路：DFS + 走迷宫 + 备忘录
     */
    // 上下左右四个方向
    int[][] dirs = new int[][]{{1, 0}, {-1, 0}, {0, 1}, {0, -1}};

    public int longestIncreasingPath(int[][] matrix) {
        int m = matrix.length;
        int n = matrix[0].length;

        // 备忘录
        int[][] memo = new int[m][n];

        // 每个点都要作为起始点遍历一下
        int ans = 0;
        for (int i = 0; i < m; i++) {
            for (int j = 0; j < n; j++) {
                // 已经遍历过的就不用遍历了
                if (memo[i][j] == 0) {
                    ans = Math.max(ans, dfs(matrix, m, n, i, j, memo));
                }
                // 这里为什么不用再比较一次 ans 和 memo[i][j]呢？
                // 因为遍历前面节点的时候已经把后面的节点遍历了
                // 说明后面的节点肯定比前面的节点的最长路径短
                // 所以，不用多判断一次了
            }
        }

        return ans;
    }

    private int dfs(int[][] matrix, int m, int n, int i, int j, int[][] memo) {
        // 已经遍历过，直接返回
        if (memo[i][j] != 0) {
            return memo[i][j];
        }

        // 否则，看四个方向是否有满足条件的节点去扩散
        // 每个节点的初始路径为1
        int ans = 1;
        for (int[] dir : dirs) {
            int nextI = i + dir[0];
            int nextJ = j + dir[1];
            // 防越界、判断递增，如果是，走下一个点，继续四个方向
            if (nextI >= 0 && nextJ >= 0 && nextI < m && nextJ <n &&
                    matrix[nextI][nextJ] > matrix[i][j]) {
                ans = Math.max(ans, dfs(matrix, m, n, nextI, nextJ, memo) + 1);
            }
        }

        // 记录到缓存中，含义是 走到i，j时的最长递增路径的长度
        memo[i][j] = ans;
        return ans;
    }

}

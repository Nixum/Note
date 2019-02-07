package backtracking;

import org.junit.Test;

/**
 * 地上有一个 m 行和 n 列的方格。一个机器人从坐标 (0, 0) 的格子开始移动，
 * 每一次只能向左右上下四个方向移动一格，但是不能进入行坐标和列坐标的数位之和大于 k 的格子。
 * 例如，当 k 为 18 时，机器人能够进入方格 (35,37)，因为 3+5+3+7=18。
 * 但是，它不能进入方格 (35,38)，因为 3+5+3+8=19。请问该机器人能够达到多少个格子？
 * 比如 5行5列矩阵，其矩阵每位如下
 * 0 1 2 3 4
 * 1 2 3 4 5
 * 2 3 4 5 6
 * 3 4 5 6 7
 * 4 5 6 7 8
 * k = 2时，输出6
 */
public class MovingScopeOfRobot {

    @Test
    public void test() {
        int[][] matrix = createMatrix(5,5);
        for (int i = 0; i < 5; i++) {
            for (int j = 0; j < 5; j++) {
                System.out.print(matrix[i][j] + " ");
            }
            System.out.println();
        }
        System.out.println(movingCount(5, 5, 2));
    }

    /**
     * 思路跟矩阵路径类似，只是终止条件变成了格子的位数要和k比较
     * 当然也可以先根据行row和列col计算整个矩阵每个位置的数位之和，直接遍历了，不过就没什么意思了
     */
    private int count = 0;
    public int movingCount(int row, int col, int k) {
        if (row <= 0 || col <= 0 || k < 0)
            return 0;
        boolean[][] visited = new boolean[row][col];
        calculateScope(row, col,0 , 0, k, visited);
        return count;
    }

    public void calculateScope(int row, int col, int i, int j, int k, boolean[][] visited) {
        // 判断是否越界
        if (i >= row || j >= col || j < 0 || i < 0) {
            return ;
        }
        if (getDigitSum(i) + getDigitSum(j) <= k && visited[i][j] == false) {
            visited[i][j] = true;
            count ++;
            calculateScope(row, col, i, j + 1, k, visited);
            calculateScope(row, col, i + 1, j, k, visited);
            calculateScope(row, col, i, j - 1, k, visited);
            calculateScope(row, col, i - 1, j, k, visited);

        }
    }

    public int getDigitSum(int n) {
        int sum = 0;
        while(n > 0) {
            sum += n % 10;  // 每次取低位数
            n /= 10;    // 每次减少低位数
        }
        return sum;
    }

    public int[][] createMatrix(int row, int col) {
        int[][] matrix = new int[row][col];
        for (int i = 0; i < row; i++) {
            for (int j = 0; j < col; j++) {
                matrix[i][j] = getDigitSum(i) + getDigitSum(j);
            }
        }
        return matrix;
    }
}


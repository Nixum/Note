package array;

import org.junit.Test;

import java.util.Arrays;

/**
 * 旋转正方型矩阵，将矩阵向右旋转90度
 *   1  2  3  4                 13 9  5 1
 *   5  6  7  8     ————————>   14 10 6 2
 *   9  10 11 12                15 11 7 3
 *   13 14 15 16                16 12 8 4
 */
public class RotateMatrix {

    @Test
    public void test() {
        int[][] matrix = new int[][] {
                {1, 2, 3, 4},
                {5, 6, 7, 8},
                {9, 10, 11, 12},
                {13, 14, 15, 16}
        };
        rotate(matrix);
        for(int i = 0; i < matrix[0].length; i++) {
            System.out.println(Arrays.toString(matrix[i]));
        }
    }

    /**
     * 按层次遍历，每次按点交换
     */
    public void rotate(int[][] matrix) {
        if (matrix == null || matrix.length <= 0 || matrix[0].length <= 0) {
            return ;
        }
        int rowLength = matrix.length;      // 行的长度
        int colLength = matrix[0].length;   // 列的长度
        // 层数
        for (int i = 0; i < rowLength / 2; i++) {
            // 注意 j要从i + 1开始，不然四个顶点的旋转会出错
            for (int j = i; j < colLength - 1 - i; j++) {
                int temp = matrix[i][j];
                matrix[i][j] = matrix[rowLength - 1 - j][i];
                matrix[rowLength - 1 - j][i] = matrix[rowLength - 1 - i][colLength - 1 - j];
                matrix[rowLength - 1 - i][colLength - 1 - j] = matrix[j][colLength - 1 - i];
                matrix[j][colLength - 1 - i] = temp;
            }
            System.out.println();
        }
    }
}

package array;

import org.junit.Test;

/**
 * 输入矩阵，顺时针由外到里打印矩阵
 */
public class MatrixPrintByClockwiseOrder {

    @Test
    public void test() {
        int[][] matrix = new int[][]{{1,2,3,4},{5,6,7,8},{9,10,11,12},{13,14,15,16}};
        printByClockwiseOrder(matrix);
    }

    /**
     * 没什么好说的，一圈一圈按顺时针打印，注意下标不要越界，还有只有一行、一列、一个数字的情况
     * 或是采用跟走迷宫一样的思路，去打印即可
     */
    public void printByClockwiseOrder(int[][] matrix) {
        if (matrix == null || matrix.length <= 0 || matrix[0].length <= 0) {
            return ;
        }
        int start = 0, row = matrix.length - 1, col = matrix[0].length - 1;
        // 判断有多少圈
        while (row > start * 2 && col > start * 2) {
            int endX = col - start, endY = row - start;
            // 从左往右打印上行
            for (int i = start; i <= endX; i++) {
                System.out.print(matrix[start][i] + " ");
            }
            // 从上往下打印右列
            if (start < endY) {
                for (int i = start + 1; i <= endY; i++) {
                    System.out.print(matrix[i][endY] + " ");
                }
            }
            // 从右往左打印下行
            if (start < endX && start < endY) {
                for (int i = endX - 1; i >= start; i--) {
                    System.out.print(matrix[endY][i] + " ");
                }
            }
            // 从下往上打印左列
            if (start < endY - 1 && start < endX) {
                for (int i = endY - 1; i >= start + 1; i--) {
                    System.out.print(matrix[i][start] + " ");
                }
            }
            start++;
        }
    }
}

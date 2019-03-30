package array;

import org.junit.Test;

/**
 * 矩阵z字形打印
 * 1 2 3 4
 * 5 6 7 8
 * 9 10 11 12
 * 13 14 15 16
 *
 * 打印：1 2 5 9 6 3 4 7 10 13 14 11 8 12 15 16
 */
public class MatrixZPrint {

    @Test
    public void test() {
        int[][] matrix = new int[][] {
            {1, 2, 3, 4},
            {5, 6, 7, 8},
            {9, 10, 11, 12},
            {13, 14, 15, 16}
        };
        printOfZ(matrix);
        System.out.println();
        printOfZAllMatrix(matrix);
    }

    /**
     * 思路：以题目的行和列长度一样的矩阵为例，不翻转的情况下，
     * 每条斜线的起始点为（0,0）（0,1）（0,2）（0,3）（1,3）（2,3）（3,3）
     * 会发现是以一个点先从0，0开始，一直往右走，走到尽头往下走，走到尽头，
     * 翻转的情况下，会发现起始点是 行列坐标互换
     * 斜线的走法是 matrix[i++][j--]和matrix[j--][i++] 注意不要越界即可
     */
    public void printOfZ(int[][] matrix) {
        if (matrix == null || matrix.length <= 0 || matrix[0].length <= 0) {
            return ;
        }
        boolean reverse = true;
        for (int i = 0, j = 0; j < matrix[0].length || i < matrix.length; j++) {
            print(matrix, i, j, reverse);
            if (j == matrix[0].length - 1) {
                j--;
                i ++;
                if (i == matrix.length)
                    break;
            }
            reverse = !reverse;
        }
    }

    private void print(int[][] matrix, int i, int j, boolean reverse) {
        int start = i;
        int end = j;
        if (reverse == false) {
            while (i <= end) {
                System.out.print(matrix[i++][j--] + " ");
            }
        } else {
            while (j >= start) {
                System.out.print(matrix[j--][i++] + " ");
            }
        }
    }

    /**
     * 如果不是正方形矩阵的，则需要分两条路，一条路是从0，0开始，一直往右走到尽头后再往下走到尽头
     * 另一条是从0，0开始，先一直往下走到尽头再往右走到尽头
     * 重新使用i1,j1和i2,j2表示两条路
     */
    public void printOfZAllMatrix(int[][] matrix) {
        if (matrix == null || matrix.length <= 0 && matrix[0].length <= 0) {
            return ;
        }
        int rEnd = matrix.length - 1;
        int cEnd = matrix[0].length - 1;
        boolean isReverse = false;
        int i1 = 0;
        int j1 = 0;
        int i2 = 0;
        int j2 = 0;
        // 没走到最后的点
        while (i1 < rEnd + 1) {
            print(matrix, i1, j1, i2, j2, isReverse);
            // 注意判断的数不能互相影响
            i1 = j1 == cEnd ? i1 + 1 : i1;
            j1 = j1 == cEnd ? j1 : j1 + 1;
            j2 = i2 == rEnd ? j2 + 1 : j2;
            i2 = i2 == rEnd ? i2 : i2 + 1;
            isReverse = !isReverse;
        }
    }

    private void print(int[][] matrix, int i1, int j1,int i2, int j2, boolean reverse) {
        if (reverse == true) {
            while(i1 <= i2) {
                System.out.print(matrix[i1++][j1--] + " ");
            }
        } else {
            while(i2 >= i1) {
                System.out.print(matrix[i2--][j2++] + " ");
            }
        }
    }
}

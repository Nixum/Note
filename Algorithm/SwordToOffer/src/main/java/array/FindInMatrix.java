package array;

import org.junit.Test;

/**
 * 在一个二维数组中，每一行都按照从左到右递增的顺序排序，每一行都按照从上到下递增的顺序排序
 * 完成一个函数，输入这样的一个二维数组和一个整数，判断数组是否含有该整数
 * input:
 * 1    2   8   9
 * 2    4   9   12
 * 4    7   10  13
 * 6    8   11  15
 * 5
 * output:false
 */
public class FindInMatrix {

    @Test
    public void test(){
        // 二维数组初始化可以没声明列数，但一定要有行
        int[][] matrix = new int[][]{{1, 2, 8, 9},{2, 4, 9, 12},{4, 7, 10, 13},{6, 8, 11, 15}};
        System.out.println(isContainInMatrix(matrix, 3));

    }

    /**
     * 利用行列递增的特性，从二维数组右上角开始找，如果该数大于目标值，说明那一列的值都大于目标值，列数减1
     * 如果该数小于目标值，说明那一行的值都小于目标值，行数加1，直到最后，只剩下左下角那个数，
     * 如果那个数都不等于目标值的话，就真的不包含了
     */
    public boolean isContainInMatrix(int[][] matrix, int target) {
        if (matrix == null || matrix.length == 0 || matrix[0].length == 0)
            return false;
        int i = 0;
        int j = matrix[0].length - 1;
        while (i != matrix.length - 1 && j != 0) {
            if (matrix[i][j] > target) {
                j--;
            } else if (matrix[i][j] < target) {
                i++;
            } else {
                return true;
            }
        }
        return false;
    }

}
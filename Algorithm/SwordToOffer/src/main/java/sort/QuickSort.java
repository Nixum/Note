package sort;

import org.junit.Test;

import java.util.Arrays;

/**
 * 快排
 * 最好/平均时间复杂度 O(nlogn)  空间复杂度 O(nlogn)   最差时间复杂度O(n^2)
 */
public class QuickSort {

    @Test
    public void test() {
        int[] array = new int[]{5, 7, 4, 9, 1, 2, 6, 3, 8};
        sort(array);
        System.out.println(Arrays.toString(array));
    }

    public static void sort(int[] array){
        if (array == null || array.length <=0) {
            return ;
        }
        sort(array, 0, array.length - 1);
    }

    /** 普通快排
     *  每一趟，取第一个数字为基数，left指针指向第一个，right指针指向最后一个，
     *  从后往前开始找(一定要，否则会因为基数原因导致后面的数字被覆盖)，找到比基数小的数，将这个数赋值给left
     *  换边，从前往后开始找，找到比基数大的数，将这个数赋值给right，如此反复，直到left<right，
     *  最后将基数赋值给left，此时会出现，在基数左边的数全比基数小，在基数右边的数全比基数大，
     *  之后递归操作基数的左右两边的数组，直到left = right
     *  实质上，每次递归都会以基数为中心，小的在基数左边，大的在基数右边，当数组足够小的时候，就是有序的了
     */
    private static void sort(int[] array, int left, int right) {
        // 递归终止条件
        if (left >= right) {
            return ;
        }
        int l = left, h = right;
        int temp = array[l];
        while (l < h) {
            // 一定要先从后往前找，记得要取等号，否则无法处理重复数字
            while (array[h] >= temp && l < h) {
                h--;
            }
            array[l] = array[h];
            while (array[l] < temp && l < h) {
                l++;
            }
            array[h] = array[l];
        }
        array[l] = temp;
        sort(array, left, l - 1); // 减一是为了排除掉排好的基数，不减也没关系，会算多一次而已
        sort(array, l + 1, right);
    }
}

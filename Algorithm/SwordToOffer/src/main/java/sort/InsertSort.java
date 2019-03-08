package sort;

import org.junit.Test;

import java.util.Arrays;

/**
 * 插入排序
 * 将数组分为两部分，前一部分有序，后一部分待排序
 * 从后一部分开始取，与前一部分的数比较，插入
 *
 * 最差/平均时间复杂度 O(n^2)    最好O(n)      空间复杂度O(1)
 */
public class InsertSort {

    @Test
    public void test() {
        int[] array = new int[]{7,6,3,5,2,9,4,1,8};
        sort(array);
        System.out.println(Arrays.toString(array));
    }

    public void sort(int[] array) {
        for (int i = 1; i < array.length; i++) {
            int j = i;
            // 取i，与前i个数进行比较交换
            while (j > 0 && array[j] < array[j - 1]) {
                int temp = array[j];
                array[j] = array[j - 1];
                array[j - 1] = temp;

                j--;
            }
        }
    }
}
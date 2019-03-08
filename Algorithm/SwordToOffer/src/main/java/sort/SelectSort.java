package sort;

import org.junit.Test;

import java.util.Arrays;

/**
 * 选择排序
 * 取第i个，与i后面的数进行比较，找出这一趟中最小的，与 i 交换
 *
 * 最好/最差/时间复杂度 O(n^2)  空间复杂度 O(1)
 */
public class SelectSort {

    @Test
    public void test() {
        int[] array = new int[]{7,6,3,5,2,9,4,1,8};
        sort(array);
        System.out.println(Arrays.toString(array));
    }

    public void sort(int[] array) {
        // i 从 0 开始
        for (int i = 0; i < array.length - 1; i++) {
            int min = i;
            // 每一趟找出最小的，与第i个交换
            for (int j = i; j < array.length; j++) {
                if (array[j] < array[min]) {
                    min = j;    // 只记录下标
                }
            }
            // 最小的不是它本身
            if (min != i) {
                int temp = array[min];
                array[min] = array[i];
                array[i] = temp;
            }
        }
    }
}

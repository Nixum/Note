package sort;

import org.junit.Test;

import java.util.Arrays;

/**
 * 冒泡排序
 * 每次循环，两两比较，反序交换，把最大的移到最后面
 *
 * 最差/平均时间复杂度 O(n^2)    最好时间复杂度O(n)     空间复杂度 O(1)
 */
public class BubbleSort {

    @Test
    public void test() {
        int[] array = new int[]{7,6,3,5,2,9,4,1,8};
        sort(array);
        System.out.println(Arrays.toString(array));
    }

    public void sort(int[] array) {
        for (int i = 0; i < array.length - 1; i++) {
            // 标记此次循环是否进行了交换，如果不用交换，说明已经有序
            boolean flag = true;
            // 两两进行比较，前面的比后面大，交换，把最大的移到最后面
            for (int j = 0; j < array.length - 1 -i; j++) {
                if (array[j] > array[j + 1]) {
                    int temp = array[j];
                    array[j] = array[j + 1];
                    array[j + 1] = temp;

                    flag = false;
                }
            }
            if (flag == true)
                break;
        }
    }
}

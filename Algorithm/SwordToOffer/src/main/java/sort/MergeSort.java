package sort;

import org.junit.Test;

import java.util.Arrays;

/**
 * 归并排序
 * 给定开始下标、中间下标、结束下标，递归将数组二分，当二分到只剩一个的时候进行合并，
 * 对比两个数组，进行合并到新数组（额外空间），新数组再赋值回去，递归返回上层继续合并
 *
 * 最好/最差/平均时间复杂度 O(nlogn)  空间复杂度 O(n)
 */
public class MergeSort {

    @Test
    public void test() {
        int[] array = new int[]{7,6,3,5,2,9,4,1,8};
        sort(array);
        System.out.println(Arrays.toString(array));
    }

    public void sort(int[] array) {
        if (array == null || array.length <=0)
            return ;
        int[] temp = new int[array.length];
        sort(array, 0, array.length - 1, temp);
    }

    /**
     * 递归,每次将数组分一半，分到只剩一个的时候进行合并，每两组数组根据大小进行插入合并
     *    7,6,3,5,2,4,1,8
     *   7,6,3,5    2,4,1,8
     *  7,6  3,5   2,4   1,8
     *  7 6  3 5   2 4   1 8
     *
     *  6,7  3,5   2,4   1,8
     *   3,5,6,7    1,2,4,8
     *    1,2,3,4,5,6,7,8
     */
    private void sort(int[] array, int start, int end, int[] temp) {
        if (start >= end)
            return ;
        int mid = (start + end) / 2;
        sort(array, start, mid, temp);
        sort(array, mid + 1, end, temp);
        merge(array, start, mid, end, temp);
    }

    private void merge(int[] array, int start, int mid, int end, int[] temp) {
        int lStart = start;
        int rStart = mid + 1;
        int tIndex = 0;
        // 左右两个相对有序的数组，每次取头一个两两比较，加入到临时数组中
        while (lStart <= mid && rStart <= end) {
            if (array[lStart] <= array[rStart]) {
                temp[tIndex] = array[lStart];
                lStart ++;
            } else {
                temp[tIndex] = array[rStart];
                rStart ++;
            }
            tIndex ++;
        }
        // 将剩余的数据加入到临时数组中
        while (lStart <= mid) {
            temp[tIndex] = array[lStart];
            tIndex ++;
            lStart ++;
        }
        while (rStart <= end) {
            temp[tIndex] = array[rStart];
            tIndex ++;
            rStart ++;
        }
        // 将排列好的临时数组的值移回原数组
        tIndex = 0;
        while(start <= end) {
            array[start] = temp[tIndex];
            start ++;
            tIndex ++;
        }
    }
}

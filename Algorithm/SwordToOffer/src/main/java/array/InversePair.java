package array;

import org.junit.Test;

import java.util.Arrays;

/**
 * 在数组中的两个数字，如果前面一个数字大于后面的数字，则这两个数字组成一个逆序对。
 * 输入一个数组，求出这个数组中的逆序对的总数。
 * 比如：[7,5,6,4]，存在5个逆序对，[7,6],[7,5],[7,4],[6,4],[5,4]
 *
 */
public class InversePair {

    @Test
    public void test() {
        System.out.println(countInversePair1(new int[]{7,5,6,4}));
        int[] a1 = new int[]{7,5,6,4};
        System.out.println(countInversePair(a1));
        int[] a2 = new int[]{8,7,5,6,4};
        System.out.println(countInversePair(a2));
        System.out.println(countInversePair2(new int[]{8,7,5,6,4}));
    }

    /**
     * 暴力解法：遍历每个数字，与其之后的每个数字作比较，时间复杂度O(n^2)，空间复杂度O(1)
     */
    private int countInversePair1(int[] arr) {
        if (arr == null || arr.length <= 0) {
            return 0;
        }
        int count = 0;
        for (int i = 0; i < arr.length; i++) {
            for (int j = i + 1; j < arr.length; j++) {
                if (arr[i] > arr[j]) {
                    count++;
                }
            }
        }
        return count;
    }

    /**
     * 思路：利用归并排序解决，时间复杂度O(nlogn)，空间复杂度O(n)
     * 先把数组分隔乘子数组，统计子数组内部的逆序对数，再统计两个相邻子数组间的逆序对的数目
     * 在统计逆序对的过程中，还需要对数组进行排序，再进行统计
     */
    int count = 0;
    private int countInversePair(int[] arr) {
        if (arr == null || arr.length <= 0) {
            return 0;
        }
        sort(arr, 0, arr.length - 1, new int[arr.length]);
        int cnt = count;
        count = 0;
        return cnt;
    }

    private void sort(int[] arr, int s, int e, int[] tmpList) {
        if (s >= e) {
            return ;
        }
        int m = s + (e - s) / 2;
        sort(arr, s, m, tmpList);
        sort(arr, m + 1, e, tmpList);
        merge(arr, s, m, e, tmpList);
    }

    // [8,7,5,6,4]
    // [0,1,2,3,4]
    private void merge(int[] arr, int s, int m, int e, int[] tmpList) {
        int le = m, re = e;
        int i = tmpList.length - 1;
        while (le >= s && re > m) {
            if (arr[le] <= arr[re]) {
                tmpList[i] = arr[re];
                re--;
            } else {
                tmpList[i] = arr[le];
                count += re - m;  // nums[le] > nums[re]，说明 arr[le] > arr[m...re]
                le--;
            }
            i--;
        }
        while (re > m) {
            tmpList[i--] = arr[re--];
        }
        while (le >= s) {
            tmpList[i--] = arr[le--];
        }
        for (int j = s, ii = i + 1; j <= e; j++, ii++) {
            arr[j] = tmpList[ii];
        }
    }

    /**
     * 上面版本，带排序的
     */
    private int cnt = 0;
    public int countInversePair2(int[] nums) {
        mergeSort(nums, 0, nums.length - 1, new int[nums.length]);
        int res = cnt;
        cnt = 0;
        return res;
    }

    private void mergeSort(int[] nums, int l, int h, int[] tmp) {
        if (h - l < 1)
            return;
        int m = l + (h - l) / 2;
        mergeSort(nums, l, m, tmp);
        mergeSort(nums, m + 1, h, tmp);
        merge1(nums, l, m, h, tmp);
    }

    private void merge1(int[] nums, int l, int m, int h, int[] tmp) {
        int i = l, j = m + 1, k = l;
        while (i <= m || j <= h) {
            if (i > m)
                tmp[k] = nums[j++];
            else if (j > h)
                tmp[k] = nums[i++];
            else if (nums[i] <= nums[j])
                tmp[k] = nums[i++];
            else {
                tmp[k] = nums[j++];
                this.cnt += m - i + 1;  // nums[i] > nums[j]，说明 nums[i...mid] 都大于 nums[j]
            }
            k++;
        }
        for (k = l; k <= h; k++)
            nums[k] = tmp[k];
    }
}

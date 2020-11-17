package array;

import org.junit.Test;

/**
 * 根据输入的排序数组和数字，找出该数字出现的次数
 * 比如nums = [1, 2, 3, 3, 3, 3, 4, 6]，target = 3
 * return 4
 */
public class NumberOccurTime {

    @Test
    public void test() {
        System.out.println(getTargetOccurTime(new int[]{1, 2, 3, 3, 3, 7, 8, 9}, 3));
    }

    /**
     * 一般思路：1.使用hashMap、遍历一次即可得出结果
     * 2.遍历一次，遍历的同时记录次数即可。
     * 3.由于是排序数组，二分法查找到该元素后，向前后扫描，即可得到次数
     * 以上几种方法都需要进行遍历，时间复杂度是O(n)
     *
     * 更好的解决方案：
     * 利用二分查找，查找该元素的第一次出现的下标和最后一次出现的下标，相减即可得到
     * 查找第一次出现的下标：
     *     二分查找时，总会先找到中间数，如果中间数比k大，说明k在左半边，反之在右半边
     *     如果中间数=k，如果该数前一个数不是k，说明该数是第一个k，如果是k，说明第一个k一定在左半边
     * 查找最后一次出现的下标：
     *     只需找k+1第一次出现的位置即可
     */
    private int getTargetOccurTime(int[] nums, int k) {
        if (nums == null || nums.length <= 0) {
            return -1;
        }
        int firstIndex = getFirstIndex(nums, k);
        int lastIndex = getFirstIndex(nums, k + 1);
        if (firstIndex > -1 && lastIndex > -1) {
            return lastIndex - firstIndex;
        }
        return -1;
    }

    private int getFirstIndex(int[] nums, int k) {
        int l = 0, r = nums.length;
        while (l < r) {
            int m = l + (r - l) / 2;
            if (nums[m] == k) {
                if (m - 1 >= 0 && nums[m - 1] != k) {
                    return m;
                } else {
                    r = m;
                }
            } else if (nums[m] >= k) {
                r = m;
            } else {
                l = m + 1;
            }
        }
        return l;
    }

}

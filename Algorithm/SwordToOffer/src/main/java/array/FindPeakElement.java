package array;

/**
 * leetcode 162
 * 峰值元素是指其值严格大于左右相邻值的元素。
 * 给你一个整数数组nums，找到峰值元素并返回其索引。数组可能包含多个峰值，
 * 在这种情况下，返回 任何一个峰值 所在位置即可。
 * 你可以假设nums[-1] = nums[n] = -∞ 。
 * 你必须实现时间复杂度为 O(log n) 的算法来解决此问题。
 *
 * 输入：nums=[1,2,3,1], 输出：2
 * 输入：nums=[1，2，1，3，5，6，3]，输出：1或5
 */
public class FindPeakElement {

    public int findPeakElement(int[] arr) {
        if (arr == null || arr.length <= 0) {
            return -1;
        }
        int len = arr.length;
        if (len == 1) {
            return 0;
        }
        for (int i = 0; i < arr.length; i++) {
            if (i == 0) {
                if (arr[i] > arr[i + 1]) {
                    return i;
                }
                continue;
            }
            if (i == len - 1) {
                if (arr[i] > arr[i - 1]) {
                    return i;
                }
                continue;
            }
            if (arr[i] > arr[i - 1] && arr[i] > arr[i + 1]) {
                return i;
            }
        }
        return -1;
    }

    // 简洁写法
    public int findPeakElement2(int[] nums) {
        int n = nums.length;
        for (int i = 0; i < n; i++) {
            boolean ok = true;
            if (i - 1 >= 0) {
                if (nums[i - 1] >= nums[i])
                    ok = false;
            }
            if (i + 1 < n) {
                if (nums[i + 1] >= nums[i])
                    ok = false;
            }
            if (ok) return i;
        }
        return -1;
    }

    /**
     * 另一种方案是二分法，之所以能用二分法，是有条件的，因为题目限制了
     * 「数据长度至少为 1」、「越过数组两边看做负无穷」和「相邻元素不相等」
     * 保证了题目一定是有解的，之后就是二分的时候确定区间
     * 在题目一定有解的前提下，二分后确定的区间，一定是 arr[x] < arr[x+-1]的那一边
     * 画图二分即可知道为什么可以找到解了
     * so 时间复杂度是 O(log2n)
     */
    public int findPeakElement3(int[] nums) {
        int n = nums.length;
        int l = 0, r = n - 1;
        while(l < r) {
            int mid = (l + r) / 2;
            if (nums[mid] > nums[mid + 1]) {
                r = mid;
            } else {
                l = mid + 1;
            }
        }
        return r;
    }

}

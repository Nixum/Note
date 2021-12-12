package find;

import org.junit.Test;

/**
 * 二分查找，前提是数组需要排好序了，每次查找数组中间的值，判断大小，
 * 再在一边继续重复此操作，直到找到目标值
 * 时间复杂度O(log2n)
 */
public class BinarySearch {

    @Test
    public void test() {
        System.out.println(searchByLoop(new int[]{1,2,3,4,5,6,7,8},9));
        System.out.println(searchByRecursion(new int[]{1,2,3,4,5,6,7,8},9));
    }

    // 非递归
    public int searchByLoop(int[] args, int target) {
        if (args == null || args.length <= 0) {
            return -1;
        }
        int start = 0;      //数组下标
        int end = args.length - 1;  //数组下标
        while (start <= end) {
            int temp = start + (end - start) / 2;
            if (args[temp] < target) {
                start = temp + 1;
            } else if (args[temp] > target) {
                end = temp - 1;
            } else {
                return temp;
            }
        }
        return -1;
    }

    /**
     * 上面非递归的写法不能找到最左边界的索引，比如[1,2,2,2,3,6], 查找2，得到的结果是 2，
     * 但是在索引1的时候就能找到了，当然，也可以直接找到之后，再向左遍历，直到找到最左边界的值
     * 但是这样算法复杂度就是 O(log2n + n)了，没有直接二分查找直接找到该值高效，so引出下面这种写法
     */
    public int BinarySearchInLeftBound(int[] args, int t) {
        if (args == null || args.length <= 0) {
            return -1;
        }
        int left = 0, right = args.length - 1;
        // 当 left = right + 1 时退出循环
        while (left <= right) {
            int mid = left + (right - left) / 2;
            if (args[mid] > t) {
                right = mid - 1;
            } else if (args[mid] < t) {
                left = mid + 1;
            } else {
                // 收紧右侧区间，以锁定左侧边界
                right = mid - 1;
            }
        }
        // 需要判断left是否越界
        if (left >= args.length || args[left] != t) {
            return -1;
        }
        return left;
    }

    // 递归
    public int searchByRecursion(int[] args, int target) {
        // start和end都表示数组下标
        return recursionSearch(args, target, 0, args.length - 1);
    }

    private int recursionSearch(int[] args, int target, int start, int end) {
        if (start <= end) {
            int temp = (start + end) / 2;
            if (args[temp] < target) {
                return recursionSearch(args, target, temp + 1, end);
            } else if (args[temp] > target) {
                return recursionSearch(args, target, start, temp - 1);
            } else {
                return temp;
            }
        }
        return -1;
    }
}

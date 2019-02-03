package find;

import org.junit.Test;

/**
 * 二分查找，前提是数组需要排好序了，每次查找数组中间的值，判断大小，再在一边继续重复此操作，直到找到目标值
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
        int start = 0;      //数组下标
        int end = args.length - 1;  //数组下标
        while (start <= end) {
            int temp = (start + end) / 2;
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

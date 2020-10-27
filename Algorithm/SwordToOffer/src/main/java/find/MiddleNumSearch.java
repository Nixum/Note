package find;

import org.junit.Test;

import java.util.PriorityQueue;

/**
 * 找出无序数组的中位数
 */
public class MiddleNumSearch {

    @Test
    public void test() {
        int[] oddNums = new int[] {2, 3, 5, 1, 4, 7, 6, 9, 8};
        System.out.println(searchMiddleNum(oddNums));
        int[] evenNums = new int[] {7, 6, 9, 8, 2, 3, 5, 1, 4, 0};
        System.out.println(searchMiddleNum(evenNums));
    }

    /**
     * 中位数：数组奇数个：排序数组中间位置的数；数组偶数个：排序数组中两数的平均值
     * 思路：利用堆，将数组分成两部分，左边使用大根堆，右边使用小根堆，将数组中的数依次存入即可
     */
    private double searchMiddleNum(int[] nums) {
        if (nums == null || nums.length <= 0) {
            return -1;
        }
        PriorityQueue<Integer> left = new PriorityQueue<>((n1, n2) -> n2 - n1);
        PriorityQueue<Integer> right = new PriorityQueue<>();
        for (int i = 0; i < nums.length; i++) {
            if (i % 2 == 0) {
                // 偶数元素会插入右边的堆，但是由于该元素不一定比左边的堆里的数都大，
                // 所以要先进左边的堆排序，在poll出来加入右边的堆
                left.add(nums[i]);
                right.add(left.poll());
            } else {
                right.add(nums[i]);
                left.add(right.poll());
            }
        }
        if (nums.length % 2 == 0) {

            return (left.peek() + right.peek()) / 2.0;
        } else {
            return right.peek();
        }
    }
}

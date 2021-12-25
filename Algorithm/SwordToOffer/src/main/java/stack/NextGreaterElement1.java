package stack;

import java.util.Stack;

/**
 * 给定一个数组nums，返回一个等长结果数组，结果数组中对应索引存储着下一个更大元素，
 * 如果没有更大的元素，存-1
 * 输入，nums=[2,1,2,4,3]，返回数组[4,2,4,-1,-1]
 */
public class NextGreaterElement1 {

    /**
     * 利用栈，保证每次新元素入栈后，栈内元素保持有序，单调递增
     */
    public int[] nextGreaterElement(int[] nums) {
        int[] res = new int[nums.length];
        Stack<Integer> stack = new Stack<>();
        // 因为是要找比该元素大的下一个元素，so从后往前遍历
        // 每次元素都入栈，比较栈顶元素与前一个元素的大小，如果小，则移除，保证栈的单调性
        for (int i = nums.length - 1; i >= 0; i--) {
            while (!stack.empty() && stack.peek() <= nums[i]) {
                stack.pop();
            }
            res[i] = !stack.empty() ? -1 : stack.peek();
            stack.push(nums[i]);
        }
        return res;
    }
}

package stack;

import java.util.Stack;

/**
 * leetcode 503
 * 给定一个数组nums，返回一个等长结果数组，结果数组中对应索引存储着下一个更大元素，允许环形
 * 输入，nums=[2,1,2,4,3]，返回数组[4,2,4,-1,4]，最后一个元素3绕了一圈找到了比自己大的元素4
 */
public class NextGreaterElement2 {

    /**
     * 利用栈，保证每次新元素入栈后，栈内元素保持有序，单调递增
     * 由于允许环形寻找，常用套路就是将数组的长度翻倍存入
     * 即 2,1,2,4,3,2,1,2,4,3，利用环形数组下标 n = n % nums.len来表示
     */
    public int[] nextGreaterElement(int[] nums) {
        int len = nums.length;
        int[] res = new int[nums.length];
        Stack<Integer> stack = new Stack<>();
        for (int i = 2 * len - 1; i >= 0; i--) {
            while (!stack.empty() && stack.peek() <= nums[i % len]) {
                stack.pop();
            }
            res[i % len] = stack.empty() ? -1 : stack.peek();
            stack.push(nums[i % len]);
        }
        return res;
    }
}

package queue;

import java.util.ArrayList;
import java.util.List;

/**
 * leetcode 239
 * 给定一个数组nums和一个正整数k，有一个大小为k的窗口再nums上从左至右滑动，输出每次窗口中k个元素的最大值
 * 输入：[1,3,-1,-3,5,3,6,7]，k = 3
 * 输出：[3,3,5,5,6,7]
 * {1,3,-1},-3,5,3,6,7，= 3
 * 1,{3,-1,-3},5,3,6,7，= 3
 * 1,3,{-1,-3,5},3,6,7，= 5
 * 1,3,-1,{-3,5,3},6,7，= 5
 * 1,3,-1,-3,{5,3,6},7，= 6
 * 1,3,-1,-3,5,{3,6,7}，= 7
 */
public class MaxSlidingWindow {

    /**
     * 使用单调递增队列，每次在队列里最多维持k个数，且单调递减
     */
    public int[] maxSlidingWindow(int[] nums, int k) {
        MonotonicQueue window = new MonotonicQueue();
        List<Integer> res = new ArrayList<>();
        for (int i = 0; i < nums.length; i++) {
            // 先添加前k-1个，才开始滑动
            if (i < k - 1) {
                window.push(nums[i]);
            } else {
                // 加入新元素
                window.push(nums[i]);
                // 记录最大值
                res.add(window.max());
                // 缩小窗口，移除旧数
                window.pop(nums[i - k + 1]);
            }
        }
        return res.stream().mapToInt(Integer :: intValue).toArray();
    }
}

package stack;

import java.util.Stack;

/**
 * leetcode 739
 * 给定数组T，该数组存放最近几天的气温，返回一个等长数组，计算对于每一天，还要至少等待多少天
 * 才能等到一个暖和的气温，如果等不到，则为0
 * 输入：T=[73,74,75,71,69,76], 返回[1,1,3,2,1,0]，
 * 即第⼀天 73 华⽒度，第⼆天 74 华⽒度，⽐ 73 ⼤，所以对于第⼀天，只要等⼀天就能等到⼀个更暖和
 * 的⽓温，后⾯的同理
 */
public class DailyTemperatures {

    /**
     * 类似 NextGreaterElement1，只是把值换成了下标，通过下标相减，表示之间的天数
     */
    public int[] dailyTemperatures(int[] t) {
        int[] res = new int[t.length];
        Stack<Integer> stack = new Stack<>();
        for (int i = t.length - 1; i >= 0; i--) {
            while (!stack.empty() && t[stack.peek()] <= t[i]) {
                stack.pop();
            }
            res[i] = !stack.empty() ? 0 : stack.peek() - i;
            stack.push(i);
        }
        return res;
    }
}

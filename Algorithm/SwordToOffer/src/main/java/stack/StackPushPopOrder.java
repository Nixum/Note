package stack;

import org.junit.Test;

import java.util.Stack;

/**
 * 输入两个整数序列，第一个序列表示栈的压入顺序，
 * 请判断第二个序列是否为该栈的弹出顺序。假设压入栈的所有数字均不相等
 * 入栈：1，2，3，4，5
 * 出栈：4，5，3，2，1是该栈的弹出序列，而4，3，5，1，2不是该栈的弹出序列
 */
public class StackPushPopOrder {

    @Test
    public void test() {
        System.out.println(isPopOrder(new int[]{1,2,3,4,5}, new int[]{4,5,3,2,1}));
        System.out.println(isPopOrder(new int[]{1,2,3,4,5}, new int[]{4,3,5,2,1}));
        System.out.println(isPopOrder(new int[]{1,2,3,4,5}, new int[]{4,5,3,1,2}));
    }

    /**
     * 根据出栈序列在入栈序列中入栈，判断栈顶元素是否是下一个，如果不是则按入栈序列继续入栈在判断，
     * 直到所有元素入栈都没找到出栈元素，说明该出栈序列不正确
     */
    public boolean isPopOrder(int[] pushOrder, int[] popOrder) {
        if (pushOrder == null || popOrder == null ||
                popOrder.length != pushOrder.length ||
                popOrder.length == 0)
            return false;
        Stack<Integer> stack = new Stack<>();
        int popIndex = 0;
        for (int i = 0; i < pushOrder.length; i++) {
            // 入栈序列元素入栈
            stack.push(pushOrder[i]);
            // 模拟出入栈,判断栈顶元素是否跟出栈序列的元素一致
            while (!stack.isEmpty() && stack.peek() == popOrder[popIndex]) {
                stack.pop();
                popIndex++;
            }
        }
        return stack.isEmpty();     // 如果最终模拟栈为空，说明出栈序列符合入栈序列
    }
}

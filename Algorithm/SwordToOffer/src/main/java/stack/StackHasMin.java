package stack;

import org.junit.Test;

import java.util.Stack;

/**
 * 定义栈的数据结构，请在该类型中实现一个能够得到栈最小元素的 min 函数
 * push、pop、min函数的时间复杂度都是O(1)
 */
public class StackHasMin {

    private Stack<Integer> dataStack = new Stack<>();   // 存放入栈的数
    private Stack<Integer> minStack = new Stack<>();    // 用于存放最小值

    @Test
    public void test() {
        StackHasMin stack = new StackHasMin();
        stack.push(3);
        stack.push(4);
        stack.push(2);
        stack.push(1);
        System.out.println(stack.min());
        System.out.println(stack.pop());
        System.out.println(stack.min());
        System.out.println(stack.pop());
        System.out.println(stack.min());
        stack.push(0);
        System.out.println(stack.min());
        System.out.println(stack.pop());
    }

    /**
     * 每次进栈时跟存放最小值的栈的栈顶元素比较，
     * 如果小，就进栈，如果大，就将该小的元素进栈
     */
    public void push(int val) {
        dataStack.push(val);
        minStack.push(minStack.isEmpty() ? val : Math.min(minStack.peek(), val));
    }

    public int pop() {
        if (dataStack.isEmpty() == true && minStack.isEmpty() == true) {
            return -1;
        }
        minStack.pop();
        return dataStack.pop();
    }

    public int min() {
        if (minStack.isEmpty() == true) {
            return -1;
        }
        return minStack.peek();
    }
}

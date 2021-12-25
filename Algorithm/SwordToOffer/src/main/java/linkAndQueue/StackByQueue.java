package linkAndQueue;

import org.junit.Test;

import java.util.LinkedList;
import java.util.Queue;

/**
 * 两个队列模拟栈,并实现进栈、出栈方法
 */
public class StackByQueue {

    /**
     * 进栈，找到两个队列中不为空的一个，入队
     * 出栈，将不为空的队列出队，进入另一个空队列，直到剩最后一个，出队
     * 反正就是要保证有一个队列是空，需要注意两个队列为空时，此时出栈，抛异常
     *
     * 也可以只使用一个队列和一个变量(用来记录最后一个入队的元素)
     * 每次出栈时，将元素出队再重新入队，直到遇到记录的栈顶元素
     */
    private Queue<Integer> queueIn = new LinkedList<>();
    private Queue<Integer> queueOut = new LinkedList<>();

    @Test
    public void test() throws Exception{
        StackByQueue stack = new StackByQueue();
        int i = 0;
        while (i != 5) {
            System.out.println("push:" + i);
            stack.push(i);
            i++;
        }
        System.out.println(stack.pop());
    }

    public void push(Integer value) {
        if (queueIn.isEmpty() && queueOut.isEmpty()) {
            queueIn.offer(value);
        } else if (queueIn.isEmpty()) {
            queueOut.offer(value);
        } else if (queueOut.isEmpty()) {
            queueIn.offer(value);
        }
    }

    public Integer pop() throws Exception {
        if (queueIn.isEmpty() && !queueOut.isEmpty()) {
            while(queueOut.size() != 1) {
                Integer v = queueOut.poll();
                queueIn.offer(v);
            }
            return queueOut.poll();
        } else if(!queueIn.isEmpty() && queueOut.isEmpty()) {
            while(queueIn.size() != 1) {
                Integer v = queueIn.poll();
                queueOut.offer(v);
            }
            return queueIn.poll();
        } else {
            throw new Exception("stack is null");
        }
    }
}

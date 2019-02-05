package linkAndQueue;

import org.junit.Test;

import java.util.Stack;

/**
 * 用两个栈实现一个队列，并实现appendTail（尾部插入）和deleteHead（头部删除）方法
 */
public class QueueByStack {
    /**
     * 两个栈，stackPush用来模拟入队，stackPop用来模拟出队
     * 当需要入队时，将值加入stackPush，
     * 当需要出队时，将stackPush的值依次出栈加入到stackPop中，最后出栈，
     * 需要注意两个栈为空时，此时出队，抛异常
     */
    private Stack<Integer> stackPush = new Stack<>();
    private Stack<Integer> stackPop = new Stack<>();

    @Test
    public void test() throws Exception{
        int i = 0;
        QueueByStack queue = new QueueByStack();
        while (i != 5) {
            System.out.println("push:" + i);
            queue.appendTail(i);
            i++;
        }
        int j = 0;
        while(j != 5) {
            System.out.println(queue.deleteHead());
            j++;
        }
    }

    public void appendTail(Integer value) {
        stackPush.push(value);
    }

    public Integer deleteHead() throws Exception{
        if (stackPop.isEmpty() == true) {
            while (stackPush.isEmpty() == false) {
                stackPop.push(stackPush.pop());
            }
        }
        if (stackPop.isEmpty() == true) {
            throw new Exception("queue is null");
        }
        return stackPop.pop();
    }
}

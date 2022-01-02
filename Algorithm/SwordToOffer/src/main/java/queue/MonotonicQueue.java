package queue;

import java.util.LinkedList;

public class MonotonicQueue {
    LinkedList<Integer> q = new LinkedList<>();

    // 在队尾添加元素，如果队尾元素比当前要加入的元素小，则删除，保证队列从大到小排列
    public void push(int n) {
        while (!q.isEmpty() && q.getLast() < n) {
            q.pollLast();
        }
        q.addLast(n);
    }

    // 返回当前队列中的最大值
    public int max() {
        return q.getFirst();
    }

    // 如果队头元素是n，则删除
    public void pop(int n) {
        if (n == q.getFirst()) {
            q.pollFirst();
        }
    }
}

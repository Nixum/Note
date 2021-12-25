package link;

import org.junit.Test;

/**
 * leetcode 19
 * 给定链表，删除链表倒数第n个结点，并且返回链表的头节点
 * 要求时间复杂度为 O(N)
 */
public class DeleteLastNNode {

    @Test
    public void test() {

    }

    /**
     * 删除倒数第n个结点，就需要找到倒数第n+1个结点，然后拼接即可
     * 使用双指针相隔n+1个定位
     */
    public LinkNode delLastNthNode(LinkNode h, int n) {
        LinkNode h1 = new LinkNode();
        h1.next = h;

        LinkNode p1 = h1;
        LinkNode p2 = h1;
        int step = 0;
        while (step < n + 1) {
            p1 = p1.next;
            step++;
        }
        while (p1 != null) {
            p1 = p1.next;
            p2 = p2.next;
        }

        if (p2 != null && p2.next != null) {
            p2.next = p2.next.next;
        }
        return h1.next;
    }
}

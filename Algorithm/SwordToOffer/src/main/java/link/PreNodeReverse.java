package link;

import org.junit.Test;

/**
 * 反正前n个节点
 */
public class PreNodeReverse {

    @Test
    public void test() {
        LinkNode head = LinkNode.createLink(new Integer[]{1,2,3,4,5,6});
        head.printLink();
        System.out.println("--- 1 ---");

        LinkNode afterHead = reversePreNNode(head, 2);
        afterHead.printLink();
    }

    public LinkNode reversePreNNode(LinkNode head, int n) {
        if (head == null) {
            return null;
        }
        if (n == 0) {
            return head;
        }
        LinkNode tmp = new LinkNode();
        LinkNode h1 = head;
        while (n > 0) {
            LinkNode next = head.next;
            head.next = tmp.next;
            tmp.next = head;
            head = next;
            n--;
        }
        h1.next = head;
        return tmp.next;
    }

    LinkNode target = null;
    public LinkNode reversePreNNode2(LinkNode head, int n) {
        if (n == 1) {
            target = head.next;
            return head;
        }
        LinkNode last = reversePreNNode2(head.next, n-1);
        head.next.next = head;
        head.next = target;
        return last;
    }
}

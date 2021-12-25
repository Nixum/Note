package link;

import org.junit.Test;

/**
 * 翻转N到M之间的节点
 */
public class NToMNodeReverse {

    @Test
    public void test() {
        LinkNode head = LinkNode.createLink(new Integer[]{1,2,3,4,5,6});
        head.printLink();

        LinkNode afterHead = reverseNToM(head, 1, 2);
        afterHead.printLink();
    }

    public LinkNode reverseNToM(LinkNode head, int n, int m) {
        if (n == 1) {
            return reverseN(head, m);
        }
        head.next = reverseNToM(head.next, n - 1, m - 1);
        return head;
    }

    LinkNode target;
    public LinkNode reverseN(LinkNode head, int n) {
        if (n == 0) {
            target = head.next;
            return head;
        }
        LinkNode last = reverseN(head.next, n - 1);
        head.next.next = head;
        head.next = target;
        return last;
    }

}

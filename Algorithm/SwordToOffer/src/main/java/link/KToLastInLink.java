package link;

import org.junit.Test;

/**
 * 输出链表的倒数第k个节点
 */
public class KToLastInLink {

    @Test
    public void test() {
        LinkNode head = LinkNode.createLink(new Integer[]{1,2,3,4,5,6});
        LinkNode node = getKToLastNode(head,6);
        System.out.println(node == null ? null : node.value);
    }

    /**
     * 由于单链表的特性，只能从前往后找，只用一个指针没办法遍历一次就找到倒数第k个，
     * 如果知道链表的长度n，那么倒数第k个就是正数的第n-k+1个，如果要遍历一遍，
     * 就需要两个指针，当第一个指针到达第k个时，第二个指针开始遍历，当第一个指针到达尾部时，
     * 第二个指针就到达了倒数第k个了
     * 但是要注意的是，k的一些特殊值可能会导致程序出错
     */
    public LinkNode getKToLastNode(LinkNode head, int k) {
        if(head == null || (head.next == null && k != 1) || k <= 0)
            return null;
        LinkNode p1 = head;
        LinkNode p2 = new LinkNode(-65535, null);
        int count = 1;
        while (p1 != null) {
            if(count == k) {
                p2 = head;
            } else if (count > k) {
                p2 = p2.next;
            }
            p1 = p1.next;
            count++;
        }
        return p2.value == -65535 ? null : p2;
    }

    /**
     * 上面那样写其实不太好，毕竟多个个临时节点，可能会出错
     * 可以把一次遍历拆成两半就不会有这种问题了
     */
    public LinkNode FindKthToTail(LinkNode head, int k) {
        if (head == null)
            return null;
        LinkNode p1 = head;
        while (p1 != null && k-- > 0)
            p1 = p1.next;
        if (k > 0)
            return null;
        LinkNode p2 = head;
        while (p1 != null) {
            p1 = p1.next;
            p2 = p2.next;
        }
        return p2;
    }
}

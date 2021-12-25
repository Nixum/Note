package link;

import org.junit.Test;

/**
 * leetcode 876
 * 给定链表，返回链表中间节点
 */
public class MiddleNodeInLink {

    @Test
    public void test() {
        LinkNode h = LinkNode.createLink(new Integer[]{1,2,3,4,5,6});
        System.out.println(findMiddleNode(h).value);
        System.out.println(findMiddleNode2(h).value);

        LinkNode h2 = LinkNode.createLink(new Integer[]{1,2,3,4,5});
        System.out.println(findMiddleNode(h2).value);
        System.out.println(findMiddleNode2(h2).value);
    }

    /**
     * 快慢指针，fast指针一次走两步，slow指针一次走一步，当fast走到链表末尾时，slow指向中点
     * 当链表长度为偶数时，该解法是返回靠后的那个
     */
    public LinkNode findMiddleNode(LinkNode head) {
        LinkNode slow = head;
        LinkNode fast = head;
        while(fast != null && fast.next != null) {
            slow = slow.next;
            fast = fast.next.next;
        }
        return slow;
    }

    /**
     * 当链表长度为偶数时，该解法返回靠前那个
     */
    public LinkNode findMiddleNode2(LinkNode head) {
        LinkNode slow = head;
        LinkNode fast = head;
        while(fast != null && fast.next != null) {
            if (fast.next.next == null) {
                break;
            }
            slow = slow.next;
            fast = fast.next.next;
        }
        return slow;
    }
}

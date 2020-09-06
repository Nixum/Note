package link;

import org.junit.Test;

/**
 * 反转链表，输出链表的尾节点作为头节点
 */
public class LinkReverse {

    @Test
    public void test() {
        LinkNode head = LinkNode.createLink(new Integer[]{1,2,3,4,5,6});
        head.printLink();
        System.out.println("--- 1 ---");

        LinkNode afterHead = reverse(head);
        afterHead.printLink();

        System.out.println("--- 2 ---");
        LinkNode afterHead2 = reverse2(afterHead);
        afterHead2.printLink();

        System.out.println("--- 3 ---");
        LinkNode afterHead3 = reverse3(afterHead2);
        afterHead3.printLink();
    }

    /**
     * 头插法翻转链表
     * 在不需要额外太多空间且只遍历一次的情况下，使用额外多两个节点来,
     * 其中一个节点用于充当头插法创建链表的临时节点和另一个节点用于保存当前节点的后一个节点
     * 之后交换来达到反转
     */
    public LinkNode reverse(LinkNode head) {
        if (head == null)
            return null;
        LinkNode tempNode = new LinkNode(-65535, null);
        while (head != null) {
            LinkNode nextNode = head.next;      // 保存下一个节点
            head.next = tempNode.next;
            tempNode.next = head;
            head = nextNode;
        }
        return tempNode.next;
    }

    /**
     * 以最小单元来看 1 -> 2, head=1, head.next.next=null, 说明已经到底了, head.next会变成头节点，
     * 所以只需要让head.next.next指向当前节点head, 当前节点的next节点指向null, 就可以把链表翻转
     */
    public LinkNode reverse2(LinkNode head) {
        // 只为找出最尾节点
        if (head != null && head.next == null) {
            return head;
        }
        LinkNode last = reverse2(head.next); // last是最后的节点，翻转后变成头结点
        // 这两步才是主要的翻转步骤
        head.next.next = head; // head为当前节点
        head.next = null;
        return last;
    }

    /**
     * 非递归版本，需要额外使用pre、cur、next指针，来进行翻转
     */
    public LinkNode reverse3(LinkNode node) {
        LinkNode pre = null;
        LinkNode cur = node;
        while (cur != null) {
            LinkNode next = cur.next;
            cur.next = pre;
            pre = cur;
            cur = next;
        }
        return pre;
    }
}

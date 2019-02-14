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
        LinkNode afterHead = reverse(head);
        afterHead.printLink();
    }

    /**
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
}

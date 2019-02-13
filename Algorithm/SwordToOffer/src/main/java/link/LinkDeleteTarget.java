package link;

import link.LinkNode;
import org.junit.Test;

/**
 * 给定链表头指针和节点指针，在链表中删除该节点，要求时间复杂度为O(1)
 */
public class LinkDeleteTarget {

    @Test
    public void test() {
        LinkNode link = LinkNode.createLinkHasHead(new Integer[]{1,2,3});
        LinkNode targetNode = link.next.next.next;      // 第3个
        link.printLink();
        deleteTargetNode(link, targetNode);
        link.printLink();
    }

    /**
     * 常规解法是遍历链表，当节点的下个节点是目标节点时，
     * 将当前节点的next指针指向目标节点的下个节点，完成删除，时间复杂度是O(n)，不符合题意
     * 因为题目已经给了目标节点了，因此可以将目标节点的下一个节点的值赋值给目标节点，
     * 之后把下个节点删掉就行，但是要注意几种特殊情况：1.目标节点在尾部，这时只能从头开始遍历，删除
     * 2.链表只有一个节点，需要把该节点删除，头节点置null
     */
    public void deleteTargetNode(LinkNode link, LinkNode targetNode) {
        if (link == null || targetNode == null) {
            return ;
        }
        if (targetNode.next != null) {
            targetNode.value = targetNode.next.value;
            targetNode.next = targetNode.next.next;
        } else {    // 目标节点在最后
            LinkNode p = link;
            while (p != null) {
                if (p != null && p.next != null &&
                p.next.value == targetNode.value) {
                    p.next = null;
                    break;
                }
                p = p.next;
            }
        }

    }
}

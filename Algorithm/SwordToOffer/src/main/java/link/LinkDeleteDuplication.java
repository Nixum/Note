package link;

import link.LinkNode;
import org.junit.Test;

/**
 * 在排序链表中删除重复节点
 * 1,2,2,2,3,4,4,5
 * 删除后
 * 1,3,5
 */
public class LinkDeleteDuplication {

    @Test
    public void test() {
        LinkNode link = LinkNode.createLink(new Integer[]{1,2,2,3,4,4,5});
        deleteDuplication(link).printLink();
    }

    /**
     * 在遍历查重的时候就进行删除和连接,这里要注意第一个节点就是重复的情况
     */
    public LinkNode deleteDuplication(LinkNode head) {
        if (head == null) {
            return head;
        }
        LinkNode q = new LinkNode();
        LinkNode qhead = q;
        LinkNode p = head;
        boolean isDuplication = false;
        while (p != null) {
            // 找到一个不重复的节点的上一个节点
            while (p.next != null && p.value == p.next.value) {
                p = p.next;
                isDuplication = true;
            }
            // 移到不重复节点，继续循环
            if(isDuplication == true) {
                p = p.next;
                isDuplication = false;
                continue;
            }
            // 拼接不重复节点
            q.next = p;
            q = q.next;
            p = p.next;
        }
        return qhead.next;
    }
}

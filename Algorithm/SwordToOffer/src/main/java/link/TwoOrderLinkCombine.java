package link;

import org.junit.Test;

/**
 * 合并两个排序递增链表，使其合并后仍然递增
 */
public class TwoOrderLinkCombine {

    @Test
    public void test() {
        combine(LinkNode.createLink(new Integer[]{1,3,5}),
                LinkNode.createLink(new Integer[]{2,4,6,8,10}))
                .printLink();
    }

    /**
     * 一个临时节点，三个指针，比较两条链的第1个节点，接上临时节点，
     * 不断比较两条链的两个节点，使用第三个指针去接
     */
    public LinkNode combine(LinkNode h1, LinkNode h2) {
        if (h1 == null && h2 == null) {
            return null;
        }
        LinkNode newLink = new LinkNode(-65535, null);
        LinkNode p = newLink;
        while(h1 != null && h2 != null) {
            if (h1.value <= h2.value) {
                p.next = h1;
                h1 = h1.next;
            } else {
                p.next = h2;
                h2 = h2.next;
            }
            p = p.next;
        }
        if (h1 == null && h2 != null)
            p.next = h2;
        if (h1 != null && h2 == null)
            p.next = h1;
        return newLink.next;
    }
}

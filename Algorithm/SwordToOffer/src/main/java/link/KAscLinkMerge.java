package link;

import java.util.PriorityQueue;

/**
 * 合并k个递增链表
 */
public class KAscLinkMerge {

    /**
     * leetcode 23
     * 方法1：依次将k个链表加入最小堆中，然后一个个top出来连接即可得到一个单调递增链表
     * 方法2：因为每条链表都是递增的，so最小堆中可以只保存每条链表的头节点，最小堆中的节点数目最多是k个
     *       每top一个就把这个节点的下一个节点放进去，时间复杂度为O(N * logk)
     */
    public LinkNode merge(LinkNode[] links) {
        if (links == null || links.length <= 0) {
            return null;
        }
        PriorityQueue<LinkNode> pq = new PriorityQueue<>(links.length, (a, b) -> (a.value - b.value));
        for (LinkNode n : links) {
            if (n != null) {
                pq.add(n);
            }
        }
        LinkNode head = new LinkNode();
        LinkNode p = head;
        while (!pq.isEmpty()) {
            LinkNode n = pq.poll();
            p.next = n;
            if (n.next != null) {
                pq.add(n.next);
            }
            p = p.next;
        }
        return head.next;
    }
}

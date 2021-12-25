package link;

/**
 * 判断链表是否包含环
 */
public class JudgeCycleInLink {

    /**
     * 快慢指针，如果链表包含环，快慢指针最终将相遇，如果不包含，fast会走到null
     */
    public boolean hasCycle(LinkNode h) {
        LinkNode slow = h, fast = h;
        while (fast != null && fast.next != null) {
            slow = slow.next;
            fast = fast.next.next;
            if (slow == fast) {
                return true;
            }
        }
        return false;
    }
}

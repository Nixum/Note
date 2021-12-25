package link;

/** leetcode 26, 83
 * 给定排序链表 nums = [1, 1, 2]，返回原链表nums的前两个元素被修改为1, 2
 * 比如
 * 给定 nums = [0,0,1,1,1,2,2,3,3,4]，返回原链表nums的前5个元素被修改为 0，1，2，3，4
 */
public class RepeatItemDelete {

    public LinkNode removeRepeatItem(LinkNode head) {
        LinkNode slow = head, fast = head;
        while (fast != null) {
            if (!fast.value.equals(slow.value)) {
                slow.next = fast;
                slow = slow.next;
            }
            fast = fast.next;
        }
        if (slow != null) {
            slow.next = null;
        }
        return head;
    }

}

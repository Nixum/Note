package link;

import org.junit.Test;

/**
 * 找到链表中环的入口,要求不使用额外空间，例如
 *        <——8，7<—
 *       |        |
 * 1，2，3，4，5，6，   说明环的入口为3
 */
public class EntryInRingLink {

    @Test
    public void test() {
        LinkNode head = LinkNode.createLink(new Integer[]{1,2,3,4,5,6,7,8});
        LinkNode p1 = head;
        while(p1.next != null) {
            p1 = p1.next;
        }
        p1.next = head.next.next.next;
        LinkNode entry = findEntry(head);
        System.out.println(entry.value);
    }

    /**
     * 对问题进行分解
     * 1.判断链表中是否有环：两个指针p1、p2，一个一次走两步，一个一次走一步，由于存在环，最终就会相遇，
     * 按上面的例子，将在6相遇，如果其中一个走到null说明不存在环
     * 2.两个指针第一次相遇说明存在环，接下来就要判断什么时候是环的入口了
     * 之后p1、p2从头节点开始，p1先移动到n的位置(n为环的个数),之后两个同时移动，当p1绕完一圈时，p2到达入口
     * 关于环的个数n，只能先用p1绕一圈，计数到原来的位置时，得到n的个数
     * 这样比较麻烦，或许我们可以利用p1的速度是p2速度的两倍来简化运算，从而省略n的计数，证明如下
     * 假设入环前的节点数为x，环入口节点到相遇节点的节点数为y，相遇节点到环入口出节点为z，那么第一次相遇时，
     * 假设p1走x+2y+z步，p2走x步后x+y步，因为p1速度为p2速度的2倍，因此x+2y+z = 2(x+y),相约为x=z
     * 也就是说，当两个指针相遇时，p1指针重新从头节点出发，每次走一步，p2从相遇节点开始走，每次走一步，
     * 再次相遇时，该节点就是环的入口了
     */
    public LinkNode findEntry(LinkNode head) {
        // 保证链表节点在两个以上(不包括两个)
        if (head == null || head.next == null)
            return null;
        LinkNode p1 = head;
        LinkNode p2 = head;
        // 第一次相遇,这里要注意p1跟p2的差距是1,且要同时开始
        do {
            if (p1.next == null)
                return null;
            p1 = p1.next.next;
            p2 = p2.next;

        } while (p1 != p2);
        p1 = head;
        while(p1 != p2) {
            p1 = p1.next;
            p2 = p2.next;
        }
        return p2;
    }
}

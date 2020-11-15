package link;

import org.junit.Test;

import java.util.Stack;

/**
 * 求两个链表的第一个公共节点，比如有
 *         a1 --->  a2  -----
 *                          |
 *                         c1 ---> c2 ---> c3
 *                         |
 *    b1 ---> b2 ---> b3---
 * 第一个公共节点为c1
 */
public class FirstCommonNode {

    @Test
    public void testFun1() {
        LinkNode h1 = LinkNode.createLink(new Integer[]{1,2,3,4,5,6,7,8});
        LinkNode h2 = LinkNode.createLink(new Integer[]{11,12,13});
        h2.next.next.next = h1.next.next;
        h1.printLink();
        h2.printLink();
        LinkNode entry = getFirstCommonNode1(h1, h2);
        System.out.println(entry.value);
    }

    @Test
    public void testFun2() {
        LinkNode h1 = LinkNode.createLink(new Integer[]{1,2,3,4,5,6,7,8});
        LinkNode h2 = LinkNode.createLink(new Integer[]{11,12,13});
        h2.next.next.next = h1.next.next;
        h1.printLink();
        h2.printLink();
        LinkNode entry = getFirstCommonNode2(h1, h2);
        System.out.println(entry.value);
    }

    @Test
    public void testFun3() {
        LinkNode h1 = LinkNode.createLink(new Integer[]{1,2,3,4,5,6,7,8});
        LinkNode h2 = LinkNode.createLink(new Integer[]{11,12,13});
        h2.next.next.next = h1.next.next;
        h1.printLink();
        h2.printLink();
        LinkNode entry = getFirstCommonNode3(h1, h2);
        System.out.println(entry.value);
    }

    /**
     * 思路：
     * 1：采用判断链表中是否含有环的解决思路，使用两个指针，先遍历一条链表，当其到达尾部时，接上另外一条链表的头指针，当两个指针同时指向同一个节点时，
     *    得到第一个公共节点 时间复杂度O(m + n)
     * 2：利用两条链表有公共节点的特性，从后往前遍历，找到有分叉的节点即可。需要利用两个辅助栈, 时间/空间复杂度O(m + n)
     * 3：从方法3上进行改良，第一遍先获取两条链表的长度，第二遍先在长链表上掠过两条链表的长度差值节点，分别开始，当两个节点
     *    相同时，说明是第一个公共节点，时间复杂度O(m + n)
     */
    private LinkNode getFirstCommonNode1(LinkNode h1, LinkNode h2) {
        LinkNode l1 = h1, l2 = h2;
        while (l1 != l2) {
            l1 = (l1 == null) ? h2 : l1.next;
            l2 = (l2 == null) ? h1 : l2.next;
        }
        return l1;
    }

    private LinkNode getFirstCommonNode2(LinkNode h1, LinkNode h2) {
        Stack<LinkNode> s1 = new Stack<>();
        Stack<LinkNode> s2 = new Stack<>();
        LinkNode p1 = h1;
        LinkNode p2 = h2;
        while (p1 != null) {
            s1.push(p1);
            p1 = p1.next;
        }
        while (p2 != null) {
            s2.push(p2);
            p2 = p2.next;
        }
        LinkNode n1 = s1.pop();
        LinkNode n2 = s2.pop();
        while (n1 == n2) {
            n1 = s1.pop();
            n2 = s2.pop();
        }
        return n1.next;
    }

    private LinkNode getFirstCommonNode3(LinkNode h1, LinkNode h2) {
        LinkNode p1 = h1;
        LinkNode p2 = h2;
        int h1Len = 0;
        int h2Len = 0;
        while (p1 != null) {
            h1Len++;
            p1 = p1.next;
        }
        while (p2 != null) {
            h2Len++;
            p2 = p2.next;
        }
        LinkNode p11 = h1, p22 = h2;
        int diff = h1Len - h2Len;
        if (diff < 0) {
            diff = -diff;
            while (diff != 0) {
                p22 = p22.next;
                diff--;
            }
        } else {
            while (diff != 0) {
                p11 = p11.next;
                diff--;
            }
        }
        while(p11 != p22) {
            p11 = p11.next;
            p22 = p22.next;
        }
        return p11;
    }
}

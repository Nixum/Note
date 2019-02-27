package link;

import org.junit.Test;

/**
 * 输入一个复杂链表（每个节点中有节点值，以及两个指针，一个指向下一个节点，另一个特殊指针指向任意一个节点），
 * 返回结果为复制后复杂链表的 head
 */
public class ComplexLinkClone {

    @Test
    public void test() {
        // 构造复杂链表 0 1(3) 2(1) 3(0) 4(5) 5
        ComplexLinkNode head = new ComplexLinkNode(0);
        ComplexLinkNode p = head;
        int i = 1;
        while(i != 6) {
            ComplexLinkNode temp = new ComplexLinkNode(i);
            p.next = temp;
            p = p.next;
            i++;
        }
        head.next.randomNext = head.next.next.next;
        head.next.next.randomNext = head.next;
        head.next.next.next.randomNext = head;
        head.next.next.next.next.randomNext = head.next.next.next.next.next;

        ComplexLinkNode.print(copy(head));
    }

    /**
     * 复制一条新链表，由于有随机指向的指针存在，所以没办法一次性复制完，
     * 第一种思路：第一遍遍历复制next指针那一条，第二次再遍历一边，每找到一个随机指针指向的节点，
     *      记录该指针指向的节点离head有几步，之后在复制链表上以同样的方式将该节点的随机指针指向那个节点，
     *      这种方法的时间复杂度很高，要达到 O(n^2)
     * 第二种思路：利用HashMap,第一步复制next指针那一条，顺便把节点以原始链表节点为key，复制节点为value保存，
     *      第二步遍历原始链表，找到节点随机指针指向的节点，根据该节点在hashMap找到复制节点，
     *      将复制链表上节点的随机指针指向它
     * 第三种思路：第一步遍历原始链表next节点，每过一个节点，复制新节点，并将它连接到原节点后面，最终得到一条链表
     *      第二步：再次遍历链表，当找到一个节点有随机指针指向的节点，
     *      则其下一个值一样的节点的随机指针指向其原始节点的随机指针指向的节点的下一个节点
     *      第三部：拆分原始链表和复制链表
     */
    public ComplexLinkNode copy(ComplexLinkNode head) {
        if (head == null)
            return null;
        // 第一步：复制链表并连接
        ComplexLinkNode p = head;
        while(p != null) {
            ComplexLinkNode temp = new ComplexLinkNode(p.value);
            temp.next = p.next;
            p.next = temp;
            p = temp.next;
        }
        // 第二步：重新遍历，对复制链表的random指针赋值
        p = head;
        while (p != null) {
            ComplexLinkNode nextClone = p.next;
            if (p.randomNext != null)
                nextClone.randomNext = p.randomNext.next;
            p = nextClone.next;
        }
        // 第三步：拆分链表，偶数位置的节点为复制节点
        p = head;
        ComplexLinkNode headClone = p.next;
        while (p.next != null) {
            ComplexLinkNode nextClone = p.next;
            p.next = nextClone.next;
            p = nextClone;
        }
        return headClone;
    }
}

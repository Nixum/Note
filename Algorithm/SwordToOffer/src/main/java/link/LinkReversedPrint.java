package link;

import org.junit.Test;

import java.util.Stack;

public class LinkReversedPrint {

    @Test
    public void test() {
        LinkNode h = LinkNode.createLink(new Integer[]{1, 2, 8, 9, 2, 4, 9, 12});
        h.printLink();
        System.out.println();
        reversedPrintLink(h);

        System.out.println();
        recursionPrint(h);

        System.out.println();
        reversePrint(h);
    }

    // 先遍历的后输出，类似于栈
    public void reversedPrintLink(LinkNode link) {
        Stack<LinkNode> nodeStack = new Stack<>();
        while (link != null) {
            nodeStack.add(link);
            link = link.next;
        }
        while (!nodeStack.isEmpty()) {
            System.out.print(nodeStack.pop().value + " ");
        }
    }

    // 递归
    public void recursionPrint(LinkNode link) {
        if (link == null) {
            return ;
        }
        recursionPrint(link.next);
        System.out.print(link.value + " ");
    }

    // 利用头插法，重新构建逆序队列
    public void reversePrint(LinkNode node) {
        LinkNode head = new LinkNode();
        while (node != null) {
            LinkNode next = node.next; // 先把下个节点保存起来，用于遍历
            node.next = head.next;
            head.next = node;
            node = next;
        }
        head = head.next;
        while (head != null) {
            System.out.print(head.value + " ");
            head = head.next;
        }
    }
}

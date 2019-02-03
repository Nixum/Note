import dataStructure.LinkNode;
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
        if(link != null) {
            recursionPrint(link.next);
            System.out.print(link.value + " ");
        }
    }
}

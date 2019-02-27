package link;

public class ComplexLinkNode {

    public ComplexLinkNode next;    // 指向下个节点
    public int value;
    public ComplexLinkNode randomNext;  //指向任意节点

    public ComplexLinkNode(int value) {
        this.value = value;
    }

    public ComplexLinkNode() {
    }

    public static void print(ComplexLinkNode head) {
        if(head == null)
            System.out.println("null");
        ComplexLinkNode p = head;
        while(p != null) {
            System.out.print(p.value);
            if (p.randomNext != null)
                System.out.print("(" + p.randomNext.value + ") ");
            else
                System.out.print(" ");
            p = p.next;
        }
    }
}

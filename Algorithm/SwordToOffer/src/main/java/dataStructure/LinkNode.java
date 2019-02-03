package dataStructure;

public class LinkNode {
    public Integer value;
    public LinkNode next;

    public LinkNode() {
    }

    public LinkNode(Integer value, LinkNode next) {
        this.value = value;
        this.next = next;
    }

    // 尾插法,返回头节点
    public static LinkNode createLink(Integer[] vlist) {
        if(vlist == null || vlist.length < 0) {
            return null;
        }
        LinkNode head = new LinkNode(vlist[0],null);
        LinkNode p = head;
        for (int i = 1; i < vlist.length; i++) {
            LinkNode q = new LinkNode(vlist[i],null);
            p.next = q;
            p = p.next;
        }
        return head;
    }

    public void printLink() {
        LinkNode p = this;
        while (p != null) {
            System.out.print(p.value);
            p = p.next;
            if(p != null)
                System.out.print(", ");
        }
    }

}

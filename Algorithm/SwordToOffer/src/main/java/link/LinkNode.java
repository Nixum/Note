package link;

public class LinkNode {
    public Integer value;
    public LinkNode next;

    public LinkNode() {
    }

    public LinkNode(Integer value, LinkNode next) {
        this.value = value;
        this.next = next;
    }

    // 尾插法,返回头节点, 这里头节点和首节点都是指第一个有值的节点
    public static LinkNode createLink(Integer[] vlist) {
        if(vlist == null || vlist.length <= 0) {
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

    // 尾插法,返回头节点, 这里头节点head指向第一个有值得节点，首节点是第一个值
    public static LinkNode createLinkHasHead(Integer[] vlist) {
        if(vlist == null || vlist.length < 0) {
            return null;
        }
        LinkNode head = new LinkNode(-65535,null);
        LinkNode p = head;
        for (int i = 0; i < vlist.length; i++) {
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
        System.out.println();
    }

}

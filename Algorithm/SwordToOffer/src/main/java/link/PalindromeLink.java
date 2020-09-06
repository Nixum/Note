package link;

import com.sun.org.apache.xml.internal.security.Init;

import org.junit.Test;

/**
 * 判断单链表是否是回文
 */
public class PalindromeLink {

  @Test
  public void test() {
    LinkNode head1 = LinkNode.createLink(new Integer[]{1,2,3,4,5,6});
    System.out.println(isPalindrome(head1));

    LinkNode head2 = LinkNode.createLink(new Integer[]{1,2,3,3,2,1});
    System.out.println(isPalindrome(head2));

    LinkNode head3 = LinkNode.createLink(new Integer[]{1,2,33,2,1});
    System.out.println(isPalindrome(head3));

    System.out.println("------------");
    System.out.println(isPalindrome2(head1));
    System.out.println(isPalindrome2(head2));
    System.out.println(isPalindrome2(head3));
  }

  /**
   * 先保存头结点
   * 利用递归特性，逆序打印，同时从头开始比较
   * 注意：上一个节点比较的结果要与当前节点比较的结果相“与”
   * 时间复杂度为O(n), 空间复杂度为O(n)
   */
  private LinkNode tmpHead;
  public boolean isPalindrome(LinkNode node) {
    tmpHead = node;
    boolean res = reverseCompare(node);
    tmpHead = null;
    return res;
  }

  private boolean reverseCompare(LinkNode node) {
    if (node == null) {
      return true;
    }
    boolean res = reverseCompare(node.next);
    res = res && tmpHead.value.intValue() == node.value.intValue();
    tmpHead = tmpHead.next;
    return res;
  }

  /**
   * 利用快慢指针，找到中心节点，但要注意奇数和偶数的情况下中心节点的位置不一样
   * 找到中心节点后，将后半部分进行翻转，与前半部分进行比较
   * 时间复杂度为O(n), 空间复杂度为O(1)
   */
  public boolean isPalindrome2(LinkNode node) {
    LinkNode fast, slow;
    LinkNode preSlow = new LinkNode(); // 用于复原被翻转的后半部分
    fast = slow = node;
    while (fast != null && fast.next != null) {
      preSlow = slow;
      slow = slow.next;
      fast = fast.next.next;
    }
    // 如果fast没有指向null，说明长度为奇数，slow指向下一个节点
    if (fast != null) {
      preSlow = slow;
      slow = slow.next;
    }
    // 翻转后半部分进行比较
    LinkNode left = node;
    LinkNode right = reverse(slow);
    // 用于复原后半部的顺序
    LinkNode tmpRight = right;
    while (right != null) {
      if (left.value.intValue() != right.value) {
        preSlow.next = reverse(tmpRight);
        return false;
      }
      left = left.next;
      right = right.next;
    }
    preSlow.next = reverse(tmpRight);
    return true;
  }

  private LinkNode reverse(LinkNode node) {
    LinkNode pre = null;
    LinkNode cur = node;
    while (cur != null) {
      LinkNode next = cur.next;
      cur.next = pre;
      pre = cur;
      cur = next;
    }
    return pre;
  }
}

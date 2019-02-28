package tree;

import link.DoublyLinkNode;
import org.junit.Test;

/**
 * 输入一个二叉搜索树，将他转换为双向链表
 * 以最左边的叶子节点为头节点
 * 例如：
 *        2        --->         1<---->2<---->3
 *      1   3
 */
public class BinarySearchTreeTransfer {

    @Test
    public void test() {
        BinaryTreeNode root = BinaryTreeNode.create(new Integer[]{10,6,14,4,8,12,16});
        BinaryTreeNode doublyLink = transferDoublyLink(root);
        while(doublyLink != null) {
            System.out.print(doublyLink.value + " ");
            doublyLink = doublyLink.rightNode;
        }
    }

    /**
     * 以二叉树结点为双向链表的结点，左节点指向前置节点，右节点指向下一个节点
     * 中序遍历二叉搜索树，左节点的左指针指向前一个节点，前一个节点的右指针指向当前节点，记录当前节点
     * 根节点的左指针指向记录节点(左节点)，记录节点(左节点)的右指针指向当前节点，记录当前节点
     * 右节点的左指针指向记录节点(根节点)，记录节点(根节点)的右指针指向当前节点，记录当前节点
     */
    public BinaryTreeNode transferDoublyLink(BinaryTreeNode root) {
        if (root == null)
            return null;
        transfer(root);
        return head;
    }

    private BinaryTreeNode head;    // 记录头节点
    private BinaryTreeNode preNode;     // 记录上一个节点
    private void transfer(BinaryTreeNode root) {
        if (root == null)
            return ;
        // 遍历左子树
        transfer(root.leftNode);
        // 处理根节点
        root.leftNode = preNode;
        if (preNode != null) {
            preNode.rightNode = root;
        }
        preNode = root;
        // 中序遍历第一个叶子节点，将它赋为头节点
        if (head == null) {
            head = root;
        }
        // 遍历右子树
        transfer(root.rightNode);

    }
}

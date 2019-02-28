package tree;

import org.junit.Test;

/**
 * 输入一棵二叉树，输出该二叉树的镜像
 *      1                 1
 *    2   3     --->    3   2
 *  4  5 6  7         7  6 5 4
 */
public class MirrorBinaryTree {

    @Test
    public void test() {
        BinaryTreeNode root = BinaryTreeNode.create(new Integer[]{1, 2, 3, 4, 5, 6, 7});
        BinaryTreeNode.preOrderTraverse(root);
        System.out.println();
        reverseBinaryTree(root);
        BinaryTreeNode.preOrderTraverse(root);
    }

    /**
     * 关于树的操作一般都是使用递归，使用递归就要抽象成一次操作是什么样子的
     * 一棵最小满二叉树，它的镜像就是左右节点互换，当一棵二叉树有多个节点时，它的镜像就是每个节点的左右节点互换
     * 但是要注意，节点的左右节点为空的情况
     */
    public void reverseBinaryTree(BinaryTreeNode root) {
        if (root == null) {
            return ;
        }
        if (root.leftNode == null && root.rightNode == null) {
            return ;
        }
        BinaryTreeNode temp = root.leftNode;
        root.leftNode = root.rightNode;
        root.rightNode = temp;
        if (root.leftNode != null) {
            reverseBinaryTree(root.leftNode);
        }
        if (root.rightNode != null) {
            reverseBinaryTree(root.rightNode);
        }
    }
}

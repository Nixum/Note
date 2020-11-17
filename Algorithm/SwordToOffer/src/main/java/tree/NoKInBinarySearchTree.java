package tree;

import org.junit.Test;

/**
 * 在二叉查找树中找到第k小节点，比如有
 *       5
 *     3   7
 *   2 4  6 8
 * k = 3时，return 4
 */
public class NoKInBinarySearchTree {

    @Test
    public void test() {
        BinaryTreeNode root = BinaryTreeNode.create(new Integer[]{5, 3, 7, 2, 4, 6, 8});
//        BinaryTreeNode.inOrderTraverse(root);
        findNoKInBinarySearchTree(root, 3);
        System.out.println(res.value);
    }

    int cur = 0;
    BinaryTreeNode res;
    private void findNoKInBinarySearchTree(BinaryTreeNode root, int k) {
        if (root == null) {
            return ;
        }
        findNoKInBinarySearchTree(root.leftNode, k);
        cur++;
        if (cur == k) {
            res = root;
        }
        findNoKInBinarySearchTree(root.rightNode, k);
    }
}

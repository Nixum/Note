package tree;

import org.junit.Test;

/**
 *
 */
public class SymmetricalBinaryTree {

    @Test
    public void test() {
        BinaryTreeNode root = BinaryTreeNode.create(new int[]{1,2,2,3,4,4,3});
        BinaryTreeNode rootNoSymmetrical = BinaryTreeNode.create(new int[]{7,7,7,7,7,7});
        System.out.println(isSymmetrical(root));
        System.out.println(isSymmetrical(rootNoSymmetrical));
    }

    /**
     * 首先换图观察什么是对称二叉树，之后根据遍历来判断
     * 发现，如果一棵对称二叉树，它的前序遍历和对称前序遍历的结果相同
     * 但是需要针对如果树的节点一致的情况，这种情况它的前序遍历和对称前序遍历都是相同的，唯一不同是是否缺了节点，
     * 解决方法是把空指针节点也考虑进去，遍历的时候输出出来，在比较
     * 最优的解决办法肯定是同时遍历的，而不是先来一遍前序遍历，再来一遍对称前序遍历，再比较
     */
    public boolean isSymmetrical(BinaryTreeNode root) {
        if(root == null)
            return false;
        return isSymmetrical(root, root);
    }

    /**
     * 递归终止条件只能是不同的时候终止，相同的时候要继续递归比较
     */
    private boolean isSymmetrical(BinaryTreeNode root1, BinaryTreeNode root2) {
        if (root1 == null && root2 == null) {
            return true;
        }
        if (root1 == null || root2 == null) {
            return false;
        }
        // 判断要放在下面，防止空指针
        if (root1.value != root2.value)
            return false;
        return isSymmetrical(root1.leftNode, root2.rightNode) &&
                isSymmetrical(root1.rightNode, root2.leftNode);
    }
}

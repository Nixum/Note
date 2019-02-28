package tree;

import org.junit.Test;

/**
 * 输入两棵二叉树A和B,判断B是否是A的子树
 */
public class SubTreeInclude {

    @Test
    public void test() {
        BinaryTreeNode root = BinaryTreeNode.create(new Integer[]{8, 8, 7, 9, 2, 1, 1});
        BinaryTreeNode.preOrderTraverse(root);
        System.out.println();
        BinaryTreeNode subRoot = BinaryTreeNode.create(new Integer[]{8, 9, 2});
        System.out.println(isInclude(root, subRoot));
    }

    /**
     * 递归遍历二叉树，思路：1.比较、2.换节点
     * 判断根节点是否一致，一致：
     * 不一致：分别换根节点的左子树和右子树继续尝试，递归下去
     */
    public boolean isInclude(BinaryTreeNode root, BinaryTreeNode subTreeRoot) {
        boolean result = false;
        if (root != null && subTreeRoot != null) {
            if (root.value == subTreeRoot.value) {  // 比较，如果根节点相等
                result = isIncludeSubTreeNode(root, subTreeRoot);   // 比较叶子节点
            }
            if (result == false) {      // 换根节点的左节点
                result = isInclude(root.leftNode, subTreeRoot);
            }
            if (result == false) {      // 换根节点的右节点
                result = isInclude(root.rightNode, subTreeRoot);
            }
        }
        return result;
    }

    private boolean isIncludeSubTreeNode(BinaryTreeNode root, BinaryTreeNode subTreeRoot) {
        // 子树节点为空，说明已经比较完了
        if (subTreeRoot == null) {
            return true;
        }
        if (root == null) {
            return false;
        }
        if (root.value != subTreeRoot.value) {
            return false;
        }
        // 左右节点均和子树的相等
        return isIncludeSubTreeNode(root.leftNode, subTreeRoot.leftNode) &&
                isIncludeSubTreeNode(root.rightNode, subTreeRoot.rightNode);
    }

}

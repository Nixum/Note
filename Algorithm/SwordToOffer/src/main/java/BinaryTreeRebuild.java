import dataStructure.BinaryTreeNode;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

/**
 * 输入某二叉树的前序遍历和中序遍历结果，重建二叉树。
 * 假设输入的前序遍历和中序遍历的结果不包含重复数字
 * input：1,2,4,7,3,5,8,6    4,7,2,1,5,3,8,6
 */
public class BinaryTreeRebuild {

    private Map<Integer,Integer> inOrderIndex = new HashMap<>();

    @Test
    public void test() {
        BinaryTreeNode tree = rebuildBinaryTree(new int[]{1,2,4,7,3,5,6,8}, new int[]{4,7,2,1,5,3,8,6});
        BinaryTreeNode.preOrderTraverse(tree);
    }

    public BinaryTreeNode rebuildBinaryTree(int[] preOrder, int[] inOrder) {
        if((preOrder == null ||  preOrder.length <= 0) || (inOrder == null ||  inOrder.length <= 0)) {
            return null;
        }
        for (int i=0; i < inOrder.length; i++) {
            inOrderIndex.put(inOrder[i], i);
        }
        return build(preOrder, 0, inOrder.length - 1, 0);
    }

    /**
     * 前序遍历确定根节点，中序遍历确定左右子树，每一次递归构建一个根节点和左右子树，前序遍历一个节点将中序遍历分成两半，
     * 节点左边为左子树中序遍历，右边为右子树中序遍历
     * @param preOrder 前序遍历的数组
     * @param preL  子树的起点，在前序数组中的下标
     * @param preR  子树的终点，在前序数组中的下标
     * @param inM
     * @return
     */
    private BinaryTreeNode build(int[] preOrder, int preL, int preR, int inM) {
        System.out.println(preL + " " + preR + " " + inM);
        if (preL > preR) {
            return null;
        }
        BinaryTreeNode root = new BinaryTreeNode(preOrder[preL]);
        int inIndex = inOrderIndex.get(preOrder[preL]);
        int leftTreeSize = inIndex - inM;
        root.leftNode = build(preOrder, preL + 1, preL + leftTreeSize, inM);
        root.rightNode = build(preOrder,preL + leftTreeSize + 1, preR, inM + leftTreeSize + 1);
        return root;
    }
}

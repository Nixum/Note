package tree;

import org.junit.Test;

/**
 * 输入一个序列，判断是否是二叉搜索树的后后序遍历
 * 二叉搜索树，若它的左子树不空，则左子树上所有结点的值均小于它的根结点的值；
 * 若它的右子树不空，则右子树上所有结点的值均大于它的根结点的值；
 * 它的左、右子树也分别为二叉排序树
 * 例如：
 * 输入：5，7，6，9，11，10，8   输出：true
 * 输入：7，4，6，5   输出：false
 */
public class BinarySearchTreeJudge {

    @Test
    public void test() {
        System.out.println(isBinarySearchTree(new int[]{5,7,6,9,11,10,8}));
        System.out.println(isBinarySearchTree(new int[]{7,4,6,5}));
    }

    /**
     * 后序遍历最后一个节点是根节点，根据根节点的值，将序列分为两部分，
     * 左子树的值都比根节点小，右子树的值都比根节点大，递归左右子树，判断是否满足二叉搜索树的定义
     */
    public boolean isBinarySearchTree(int[] postOrder) {
        if (postOrder == null || postOrder.length<=0)
            return false;
        return isBinarySearchTree(postOrder, 0, postOrder.length - 1);
    }

    /**
     * @param start 划分数组，一棵树的起点
     * @param end 划分数组，一棵树的终点
     */
    private boolean isBinarySearchTree(int[] postOrder, int start, int end) {
        // 获取根节点的值
        int root = postOrder[end];
        // 找到左子树和右子树的间隔点,此时i在右子树第一个起点
        int i = start;
        for (; i < end; i++) {
            if (postOrder[i] > root)
                break;
        }
        // 在右子树中寻找，看看右子树所有节点是否都大于根节点
        int j = i;
        for (; j < end; j++) {
            if (postOrder[j] < root)
                return false;
        }
        // 判断左子树是否满足条件
        boolean isLeftTrue = true;
        if (i > start)
            isLeftTrue = isBinarySearchTree(postOrder, start, i - 1);
        // 判断右子树是否满足条件
        boolean isRightTrue = true;
        if (i < end)
            isLeftTrue = isBinarySearchTree(postOrder, i, end - 1);

        return isLeftTrue && isRightTrue;
    }
}

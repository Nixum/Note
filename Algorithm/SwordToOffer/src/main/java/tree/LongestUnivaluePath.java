package tree;

/**
 * leetcode 687
 * 给定一个二叉树的root，返回最长的路径的长度 ，这个路径中的每个节点具有相同值。
 * 这条路径可以经过也可以不经过根节点。
 * 两个节点之间的路径长度由它们之间的边数表示。
 * 比如：
 *       5
 *    4     5
 *  1   1      5
 *  root=[5,4,5,1,1,5], 输出：2
 *
 *       1
 *    4     5
 *  4   4      5
 *  root=[1,4,5,4,4,5], 输出：2
 */
public class LongestUnivaluePath {

    /**
     * 思路：
     * 由于只需要输出结果，无需打印节点，所以算比较简单的
     * 使用后序遍历模板，判断左/右节点是否等于根节点，如果是，说明可以累加
     * 回到根节点，左右节点结果之和为最终结果
     * 当前结果则是取左右节点中大的那个返回给上层
     */
    int result = 1;

    public int getLongestUnivaluePath(BinaryTreeNode root) {
        longestUnivaluePath(root);
        return result - 1;
    }

    private int longestUnivaluePath(BinaryTreeNode root) {
        if (null == root) {
             return 0;
        }
        int l = longestUnivaluePath(root.leftNode);
        int r = longestUnivaluePath(root.rightNode);
        int lNum = root.leftNode != null && root.leftNode.value == root.value ? l : 0;
        int rNum = root.rightNode != null && root.rightNode.value == root.value ? r : 0;
        result = Math.max(result, lNum + rNum + 1);
        return Math.max(lNum, rNum) + 1;
    }
}

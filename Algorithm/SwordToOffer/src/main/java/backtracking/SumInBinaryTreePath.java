package backtracking;

import org.junit.Test;
import tree.BinaryTreeNode;

import java.util.ArrayList;
import java.util.List;

/**
 * 输入二叉树和一个整数，打印二叉树节点值和为输入整数的路径，
 * 从根节点开始到叶子节点所经过的节点所形成的路径
 */
public class SumInBinaryTreePath {

    @Test
    public void test() {
        BinaryTreeNode root = BinaryTreeNode.create(new int[]{10, 5, 12, 4, 7});
        printPathOfSumInBinaryTree(root, 15);
    }

    public void printPathOfSumInBinaryTree(BinaryTreeNode root, int sum) {
        if (root == null)
            return ;
        backtracking(root, sum, new ArrayList<Integer>());
    }

    /**
     * 回溯法解决
     * 从根节点开始，每进入一个节点，将sum值减去该节点的值，判断sum是否=0，
     * 从左子树开始，直到叶子节点，如果sum值仍然不=0，则进入右节点，继续判断，如果右节点已经遍历到叶子节点还没找到路径，
     * 则继续向上回溯，将原本加入路径的节点退出，换上个节点的右子树，继续向下寻找路径，直到sum=0，或者到达叶子节点
     */
    // 路径可能有多条，因此需要一个全局变量来重置路径
    private List<Integer> finalPath = new ArrayList<>();
    private void backtracking(BinaryTreeNode root, int sum, List<Integer> path) {
        if (root == null)
            return ;
        path.add(root.value);
        sum = sum - root.value;
        if (sum == 0) {
            finalPath.addAll(path);
            System.out.println(finalPath);
            finalPath = new ArrayList<>();
        } else {
            backtracking(root.leftNode, sum, path);
            backtracking(root.rightNode, sum, path);
        }
        path.remove(path.size() - 1);
    }
}

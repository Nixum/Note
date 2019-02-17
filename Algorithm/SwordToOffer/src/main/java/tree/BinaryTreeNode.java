package tree;

import java.util.LinkedList;
import java.util.Queue;

public class BinaryTreeNode {

    public int value;
    public BinaryTreeNode leftNode;
    public BinaryTreeNode rightNode;

    public BinaryTreeNode() {
    }

    public BinaryTreeNode(int value) {
        this.value = value;
    }

    // 传入满二叉树字符串，空节点使用#或者0代替,按照层次输入节点
    public static BinaryTreeNode create(int[] tree){
        BinaryTreeNode root = new BinaryTreeNode();
        int i = 0;
        build(root, i, tree.length, tree);
        return root;
    }

    private static void build(BinaryTreeNode root, int i, int length, int[] str) {
        if (i >= length) {
            return ;
        }
        root.value = str[i];
        if (2*i+1 < length) {
            root.leftNode = new BinaryTreeNode();
        }
        if (2*i+2 < length) {
            root.rightNode = new BinaryTreeNode();
        }
        build(root.leftNode, 2*i+1, length, str);
        build(root.rightNode, 2*i+2, length, str);
    }

    /**
     * 前序遍历
     */
    public static void preOrderTraverse(BinaryTreeNode root) {
        if (root == null)
            return;
        System.out.print(root.value + " ");
        preOrderTraverse(root.leftNode);
        preOrderTraverse(root.rightNode);
    }

    /**
     * 对称前序遍历
     */
    public static void symmetricalPreOrderTraverse(BinaryTreeNode root) {
        if (root == null)
            return;
        System.out.print(root.value + " ");
        symmetricalPreOrderTraverse(root.rightNode);
        symmetricalPreOrderTraverse(root.leftNode);
    }

    /**
     * 中序遍历
     */
    public static void inOrderTraverse(BinaryTreeNode root) {
        if (root == null)
            return;
        preOrderTraverse(root.leftNode);
        System.out.print(root.value + " ");
        preOrderTraverse(root.rightNode);
    }

    /**
     * 后序遍历
     */
    public static void postOrderTraverse(BinaryTreeNode root) {
        if (root == null)
            return;
        preOrderTraverse(root.leftNode);
        preOrderTraverse(root.rightNode);
        System.out.print(root.value + " ");
    }

    /**
     * 层次遍历
     * 从上到下打印二叉树节点，每次打印一个节点，如果该节点有叶子节点，
     * 则把该节点的子节点入队，接下来出队头节点，重复打印操作
     */
    public static void levelOrderTraverse(BinaryTreeNode root) {
        if (root == null)
            return ;
        Queue<BinaryTreeNode> queue = new LinkedList<>();
        queue.offer(root);
        while (queue.isEmpty() == false) {
            BinaryTreeNode node = queue.poll();
            System.out.print(node.value + " ");
            if (node.leftNode != null)
                queue.offer(node.leftNode);
            if (node.rightNode != null)
                queue.offer(node.rightNode);
        }
    }

    public static void main(String[] args) {
        BinaryTreeNode root = BinaryTreeNode.create(new int[]{1,2,3,4,5,6,7});

        System.out.print("前序：");BinaryTreeNode.preOrderTraverse(root);
        System.out.println();
        System.out.print("对称前序：");BinaryTreeNode.symmetricalPreOrderTraverse(root);
        System.out.println();
        System.out.print("中序：");BinaryTreeNode.inOrderTraverse(root);
        System.out.println();
        System.out.print("后序：");BinaryTreeNode.postOrderTraverse(root);
        System.out.println();

        System.out.print("层次：");levelOrderTraverse(root);
    }
}

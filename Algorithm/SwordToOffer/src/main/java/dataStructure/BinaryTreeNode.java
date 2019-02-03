package dataStructure;

public class BinaryTreeNode {

    public int value;
    public BinaryTreeNode leftNode;
    public BinaryTreeNode rightNode;

    public BinaryTreeNode() {
    }

    public BinaryTreeNode(int value) {
        this.value = value;
    }

    // 传入满二叉树字符串，空节点使用#代替
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

    public static void preOrderTraverse(BinaryTreeNode root) {
        if (root == null)
            return;
        System.out.print(root.value);
        preOrderTraverse(root.leftNode);
        preOrderTraverse(root.rightNode);
    }

    public static void inOrderTraverse(BinaryTreeNode root) {
        if (root == null)
            return;
        preOrderTraverse(root.leftNode);
        System.out.print(root.value);
        preOrderTraverse(root.rightNode);
    }

    public static void postOrderTraverse(BinaryTreeNode root) {
        if (root == null)
            return;
        preOrderTraverse(root.leftNode);
        preOrderTraverse(root.rightNode);
        System.out.print(root.value);
    }

    public static void main(String[] args) {
        BinaryTreeNode root = BinaryTreeNode.create(new int[]{1,2,3,4,5,6,7});

        BinaryTreeNode.preOrderTraverse(root);
        System.out.println();
        BinaryTreeNode.inOrderTraverse(root);
        System.out.println();
        BinaryTreeNode.postOrderTraverse(root);
    }
}

package tree;

public class BinaryTreeNodeParent {

    public int value;
    public BinaryTreeNodeParent parentNode;
    public BinaryTreeNodeParent leftNode;
    public BinaryTreeNodeParent rightNode;

    public BinaryTreeNodeParent() {
    }

    public BinaryTreeNodeParent(int value, BinaryTreeNodeParent parentNode, BinaryTreeNodeParent leftNode, BinaryTreeNodeParent rightNode) {
        this.value = value;
        this.parentNode = parentNode;
        this.leftNode = leftNode;
        this.rightNode = rightNode;
    }

}

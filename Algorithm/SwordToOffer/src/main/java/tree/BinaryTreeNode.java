package tree;

import java.util.LinkedList;
import java.util.Queue;
import java.util.Stack;

public class BinaryTreeNode {

    public Integer value;
    public BinaryTreeNode leftNode;
    public BinaryTreeNode rightNode;

    public BinaryTreeNode() {
    }

    public BinaryTreeNode(int value) {
        this.value = value;
    }

    // 传入满二叉树字符串，空节点使用#或者0代替,按照层次输入节点
    public static BinaryTreeNode create(Integer[] tree){
        return build(0, tree.length, tree);
    }

    private static BinaryTreeNode build(int i, int length, Integer[] str) {
        if (i >= length || str[i] == null) {
            return null;
        }
        BinaryTreeNode root = new BinaryTreeNode();
        root.value = str[i];
        root.leftNode = build(2*i+1, length, str);
        root.rightNode = build(2*i+2, length, str);
        return root;
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
        inOrderTraverse(root.leftNode);
        System.out.print(root.value + " ");
        inOrderTraverse(root.rightNode);
    }

    /**
     * 后序遍历
     */
    public static void postOrderTraverse(BinaryTreeNode root) {
        if (root == null)
            return;
        postOrderTraverse(root.leftNode);
        postOrderTraverse(root.rightNode);
        System.out.print(root.value + " ");
    }

    /**
     * 层次遍历 bfs广度优先遍历
     * 从上到下打印二叉树节点，每次打印一个节点，如果该节点有叶子节点，
     * 则把该节点的子节点入队，接下来出队头节点，重复打印操作
     */
    public static void levelOrderTraverse(BinaryTreeNode root) {
        if (root == null)
            return ;
        Queue<BinaryTreeNode> queue = new LinkedList<>();
        queue.offer(root);
        while (!queue.isEmpty()) {
            BinaryTreeNode node = queue.poll();
            System.out.print(node.value + " ");
            if (node.leftNode != null)
                queue.offer(node.leftNode);
            if (node.rightNode != null)
                queue.offer(node.rightNode);
        }
    }

    /**
     * dfs深度优先遍历二叉树，其实就是一个先序遍历
     * 利用栈，先进右子树再进左子树
     */
    public static void dfsTraverse(BinaryTreeNode root) {
        if (root == null)
            return ;
        Stack<BinaryTreeNode> stack = new Stack<>();
        stack.push(root);
        while (!stack.isEmpty()) {
            BinaryTreeNode temp = stack.pop();
            System.out.print(temp.value + " ");
            if (temp.rightNode != null) {
                stack.push(temp.rightNode);
            }
            if (temp.leftNode != null)
                stack.push(temp.leftNode);
        }
    }

    /**
     * 非递归，先序遍历
     * 循环内，左结点边打印边进栈，当左结点为null时，更换成右结点，继续循环
     */
    public static void preOrderTraverseNoRecursion(BinaryTreeNode root) {
        if (root == null)
            return ;
        Stack<BinaryTreeNode> stack = new Stack<>();
        while (root != null || !stack.isEmpty()) {
            while (root != null) {
                System.out.print(root.value + " ");
                stack.push(root);
                root = root.leftNode;
            }
            if (!stack.isEmpty()) {
                root = stack.pop().rightNode;
            }
        }
    }

    // 统一风格写法，先序遍历
    public static void preOrderTraverseNoRecursion2(BinaryTreeNode root) {
        if (root == null)
            return ;
        Stack<BinaryTreeNode> stack = new Stack<>();
        stack.push(root);
        while(!stack.empty()) {
            BinaryTreeNode n = stack.pop();
            if (n != null) {
                if (n.rightNode != null) {
                    stack.push(n.rightNode);
                }
                if (n.leftNode != null) {
                    stack.push(n.leftNode);
                }
                stack.push(n);
                stack.push(null);
            } else {
                BinaryTreeNode r = stack.pop();
                System.out.print(r.value + " ");
            }
        }
    }

    /**
     * 非递归，中序遍历
     * 循环，左结点不断进栈，null时，出栈打印，更换成右结点，继续循环
     */
    public static void inOrderTraverseNoRecursion(BinaryTreeNode root) {
        if (root == null)
            return ;
        Stack<BinaryTreeNode> stack = new Stack<>();
        while (root != null || !stack.isEmpty()) {
            while (root != null) {
                stack.push(root);
                root = root.leftNode;
            }
            if (!stack.isEmpty()) {
                root = stack.pop();
                System.out.print(root.value + " ");
                root = root.rightNode;
            }
        }
    }

    // 统一风格写法，中序遍历
    public static void inOrderTraverseNoRecursion2(BinaryTreeNode root) {
        if (root == null)
            return ;
        Stack<BinaryTreeNode> stack = new Stack<>();
        stack.push(root);
        while (!stack.isEmpty()) {
            BinaryTreeNode cur = stack.pop();
            if (cur != null) {
                if (cur.rightNode != null) {
                    stack.push(cur.rightNode);
                }
                stack.push(cur);
                stack.push(null);
                if (cur.leftNode != null) {
                    stack.push(cur.leftNode);
                }
            } else {
                BinaryTreeNode r = stack.pop();
                System.out.print(r.value + " ");
            }
        }
    }

    /**
     * 非递归，后序遍历
     * 循环，左结点不断进栈，当左结点为空时，进入二层循环，判断右结点是否被访问过，如果被访问过，
     * 则出栈打印结点，不然更换右结点，退出二层循环，继续一层循环
     */
    public static void postOrderTraverseNoRecursion(BinaryTreeNode root) {
        if (root == null)
            return ;
        Stack<BinaryTreeNode> stack = new Stack<>();
        while (root != null || !stack.isEmpty()) {
            while (root != null) {
                stack.push(root);
                root = root.leftNode;
            }
            BinaryTreeNode preNode = null;  // 记录上次访问的结点
            while (!stack.isEmpty()) {
                root = stack.peek();
                // 针对根结点来看的，判断其右子树是否被访问过，是就打印根节点
                if (root.rightNode == preNode) {
                    root = stack.pop();
                    System.out.print(root.value + " ");
                    preNode = root;
                } else {
                    root = root.rightNode;
                    break;
                }
                if (stack.isEmpty())
                    return ;
            }
        }
    }

    // 统一风格写法，后序遍历
    public static void postOrderTraverseNoRecursion2(BinaryTreeNode root) {
        if (root == null)
            return ;
        Stack<BinaryTreeNode> stack = new Stack<>();
        stack.push(root);
        while (!stack.isEmpty()) {
            BinaryTreeNode cur = stack.pop();
            if (cur != null) {
                stack.push(cur);
                stack.push(null);
                if (cur.rightNode != null) {
                    stack.push(cur.rightNode);
                }
                if (cur.leftNode != null) {
                    stack.push(cur.leftNode);
                }
            } else {
                BinaryTreeNode r = stack.pop();
                System.out.print(r.value + " ");
            }
        }
    }

    /**
     * 二叉树的深度
     */
    public static int getTreeDepth(BinaryTreeNode root) {
        // 比较当前节点的左节点高度和右节点高度
        return root == null ? 0 : 1 + Math.max(getTreeDepth(root.leftNode), getTreeDepth(root.rightNode));
    }

    /**
     * 判断是否是平衡二叉树：左右子树高度差不超过1
     */
    private static boolean isBalance = true;
    public static boolean isBalancedTree(BinaryTreeNode root) {
        height(root);
        boolean balance = isBalance;
        isBalance = true;
        return balance;
    }

    private static int height(BinaryTreeNode root) {
        if (root == null || !isBalance) {
            return 0;
        }
        int left = height(root.leftNode);
        int right = height(root.rightNode);
        if (Math.abs(left - right) > 1) {
            isBalance = false;
        }
        return 1 + Math.max(left, right);
    }

    public static void main(String[] args) {
        BinaryTreeNode root = BinaryTreeNode.create(new Integer[]{1,2,3,4,5,6,7});

        System.out.print("前序：");BinaryTreeNode.preOrderTraverse(root);
        System.out.println();
        System.out.print("对称前序：");BinaryTreeNode.symmetricalPreOrderTraverse(root);
        System.out.println();
        System.out.print("中序：");BinaryTreeNode.inOrderTraverse(root);
        System.out.println();
        System.out.print("后序：");BinaryTreeNode.postOrderTraverse(root);
        System.out.println();

        System.out.print("层次：");levelOrderTraverse(root);
        System.out.println();
        System.out.print("深度dfs：");dfsTraverse(root);
        System.out.println();

        System.out.print("先序非递归："); preOrderTraverseNoRecursion(root);
        System.out.println();
        System.out.print("中序非递归："); inOrderTraverseNoRecursion(root);
        System.out.println();
        System.out.print("后序非递归："); postOrderTraverseNoRecursion(root);
        System.out.println();

        System.out.print("先序非递归2："); preOrderTraverseNoRecursion2(root);
        System.out.println();
        System.out.print("中序非递归2："); inOrderTraverseNoRecursion2(root);
        System.out.println();
        System.out.print("后序非递归2："); postOrderTraverseNoRecursion2(root);
        System.out.println();

        System.out.println("深度：" + getTreeDepth(root));
    }
}

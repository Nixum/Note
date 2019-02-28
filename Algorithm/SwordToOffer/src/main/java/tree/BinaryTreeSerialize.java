package tree;

import org.junit.Test;

import java.util.regex.Pattern;

/**
 * 序列化和反序列化二叉树
 * 序列化规则为
 *        1
 *     2    3
 *   4    5   6
 *   序列化为 1,2,4,$,$,$,3,5,$,$,6,$,$
 *   即对二叉树使用前序遍历，null用$替代
 *  反序列化为将该字符串转化为二叉树
 */
public class BinaryTreeSerialize {

    @Test
    public void test() {
        BinaryTreeNode root = BinaryTreeNode.create(new Integer[]{1,2,3,4,null,5,6});
        StringBuffer result = serialize(root);
        System.out.println(result.toString());
        BinaryTreeNode proot = deSerialize(result.toString());
        BinaryTreeNode.preOrderTraverse(proot);
    }

    private static StringBuffer serializeString = new StringBuffer();

    /**
     * 序列化，跟前序遍历其实很像
     */
    public static StringBuffer serialize(BinaryTreeNode root) {
        if (root == null) {
            return serializeString.append("$");
        }
        serializeString.append(root.value);
        serialize(root.leftNode);
        serialize(root.rightNode);
        return serializeString;
    }

    /**
     * 反序列化
     */
    public static BinaryTreeNode deSerialize(String serializeStr) {
        if(serializeStr == null || serializeStr.length() == 0 || "".equals(serializeStr.trim()))
            return null;
        char[] charArray = serializeStr.toCharArray();
        BinaryTreeNode root = deSerialize(charArray);
        i = 0;
        return root;
    }

    // 记录数组的位置，因为java只有值传递，如果作为参数的话，递归会导致i回到原来的值导致出错
    private static int i = 0;
    private static BinaryTreeNode deSerialize(char[] serializeStr) {
        if (i >= serializeStr.length)
            return null;

        if (serializeStr[i] == '$') {
            i++;
            return null;
        }
        BinaryTreeNode root = new BinaryTreeNode(serializeStr[i] - '0');
        i++;
        root.leftNode = deSerialize(serializeStr);
        root.rightNode = deSerialize(serializeStr);
        return root;
    }
}

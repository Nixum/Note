package sort;

import org.junit.Test;

import java.util.Arrays;

/**
 * 堆排序
 * 两个步骤：1.创建初始堆    2.头节点与末结点交换，调整满足大根堆定义
 * 如何创建初始堆：
 *      从第一个非叶子结点开始，比较该结点的左右孩子，大的结点与其父结点交换，
 *      沿着交换的叶子结点的左右孩子继续判断是否满足大根堆定义，调整；
 *      第一个非叶子结点的下标减1，重复调整操作，直至调整满足大根堆定义
 * 如何调整：
 *      从数组最后一个元素开始，之后每次减1，与根结点交换
 *      根结点为最大值，将其与最后一个结点交换，此时最后一个结点排最后了，
 *      然后从根结点开始，调整至满足大根堆定义，此时根结点为次最大值
 *      根结点与倒数第二个结点交换，调整，如此反复，即完成排序
 *
 * 最差/平均时间复杂度 O(nlogn)    最好时间复杂度O(nlogn)       空间复杂度 O(1)
 */
public class HeapSort {

    @Test
    public void test() {
        int[] array = new int[]{7,6,3,5,2,9,4,1,8};
        sort(array);
        System.out.println(Arrays.toString(array));
    }

    /**
     * 将数组看成树，即数组下标i为根结点，其左结点为 2*i+1， 右结点为 2*i+2（左节点下标+1）
     * 初始数组
     *          7
     *      6      3
     *    5   2  9   4
     *  1  8
     * 创建初始堆，从第一个非叶子结点 5 开始，之后是3，6，7
     * 第一次循环                第二次循环
     *          7                       7
     *      6      3                6      9
     *    8   2  9   4            8   2  3   4
     *  1  5                    1  5
     * 第三次循环                第四次循环
     *          7                       9
     *      8      9                8      7
     *    6   2  3   4            6   2  3   4
     *  1  5                    1  5
     * 开始根结点与末结点交换，调整成堆，之后排除末结点
     * 第一次循环
     * 交换                     调整
     *          5                       8
     *      8      7                6      7
     *    6   2  3   4            5   2  3   4
     *  1  9                    1  9
     * 第二次循环
     * 交换                     调整
     *          1                       7
     *      6      7                6      3
     *    5   2  3   4            5   2  1   4
     *  8  9                    8  9
     * 第三次循环
     * 交换                     调整
     *          4                       6
     *      6      3                5      3
     *    5   2  1   7            4   2  1   7
     *  8  9                    8  9
     * 第四次循环
     * 交换                     调整
     *          1                       5
     *      5      3                4      3
     *    4   2  6   7            1   2  6   7
     *  8  9                    8  9
     * 以此类推...
     */
    public void sort(int[] array) {

        // 创建初始堆，从左边第一个非叶子结点开始，直到根结点i，（因为是完全二叉树，左子树结点肯定比右子树多的，所以才可以这么做）
        for (int i = array.length / 2 - 1; i >= 0; i--) {
            adjust(array, i, array.length);
        }
        // 根结点与最后结点交换，继续调整成满足大根堆的定义
        for (int i = array.length - 1; i > 0; i--) {
            // 将根结点与末结点交换
            int temp = array[0];
            array[0] = array[i];
            array[i] = temp;
            // 继续调整余下结点成大根堆
            adjust(array, 0, i);
        }
    }

    /**
     * 调整堆，使其满足大根堆的定义，即根结点大于左右结点
     * 注意这是在数组已经是大根堆的前提下
     */
    private void adjust(int[] array, int rootIndex, int length) {
        int temp = array[rootIndex];
        // 从根结点的左结点开始进行判断，之后沿着有进行交换的那一分支继续找子节点比较、交换
        for (int i = 2 * rootIndex + 1; i < length; i = 2 * i + 1) {
            // 左右结点比较，找出大的结点
            if (i + 1 < length && array[i] < array[i + 1]) {
                i ++;   // 移向右结点
            }
            // 大的结点和根节点交换
            if (array[i] > temp) {
                array[rootIndex] = array[i];
                rootIndex = i;
            } else {
                // 直到左右结点都比根结点小
                break;
            }
        }
        // 将根结点下沉到满足大根堆定义的最大位置
        array[rootIndex] = temp;
    }
}

package array;

import org.junit.Test;

import java.util.Arrays;

/**
 * 输入一个整数数组，实现一个函数来调整该数组中数字的顺序，
 * 使所有奇数位于数组前半部分，偶数位于数组后半部分
 *
 */
public class NumberAdjustInArray {

    @Test
    public void test() {
        int[] a = new int[]{6,2,3,1,4,5};
        adjust(a);
        System.out.println(Arrays.toString(a));
    }

    /**
     * 思路有点像快排的一趟排列，前后两个指针移动，交换
     * 扩展版其实就是将两个while里的条件抽成一个函数，之后子类基础此类，重写该条件函数，
     * 在调用此方法就能实现根据不同条件移动数字了
     */
    public void adjust(int[] array) {
        if (array == null || array.length <=0)
            return ;
        int i = 0;
        int j = array.length - 1;
        while (i < j) {
            while (array[i] % 2 != 0 && i < j) {
                i++;
            }
            while (array[j] % 2 == 0 && i < j) {
                j--;
            }
            // 当i和j相等时就不用换了
            if (i < j) {
                int temp = array[i];
                array[i] = array[j];
                array[j] = temp;
            }
        }
    }
}

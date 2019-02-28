package array;

import org.junit.Test;

/**
 * 给定一个数组，找出里面一个数，其出现的次数多于数组长度的一半
 * 注意有可能没有这种数字
 * 输入：1，2，3，2，2，2，5，4，2
 * 输出：2
 */
public class NumSumOverHalf {

    @Test
    public void test() {
        System.out.println(find(new int[]{1,2,3,2,2,2,5,4,2}));
    }

    /**
     * 第一反应是使用hashMap去做，但是要用到额外的空间，网上找了之后发现是多数投票问题
     * 利用该数字出现的次数有数组长度一半多的特点，也就是说该数字出现的次数比其他数出现的次数之和还要多
     * 在遍历数组的时候保存两个值，一个是数字，另一个是数字出现的次数，遍历的时候，
     * 如果下一个数字和保存的值相同，次数加1，否则次数减1，如果次数为0，则保存该数字并设置次数为1，
     * 到最后要找的数字肯定是最后一次把次数设置为1的次数
     * 这是假设该数存在的时候这么做的，之后再判断该数出现的次数是否超过长度一半
     * 这也是多数投票问题
     */
    public int find(int[] array) {
        if (array == null || array.length <= 0)
            return -1;
        int temp = array[0], count = 1;
        for (int i = 1; i < array.length; i++) {
            if (array[i] == temp) {
                count ++;
            } else {
                count --;
            }
            if (count == 0) {
                temp = array[i];
                count = 1;
            }
        }
        count = 0;
        for (int i = 1; i < array.length; i++) {
            if (array[i] == temp)
                count ++;
        }
        return count * 2 > array.length ? temp : -1;
    }

}

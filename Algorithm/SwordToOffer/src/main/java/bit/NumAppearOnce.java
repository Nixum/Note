package bit;

import org.junit.Test;

import java.util.Arrays;

/**
 * 一个整型数组里除了两个数字之外，其他的数字都出现了两次，找出这两个数。
 * 比如有[2,4,3,6,3,2,5,5]，输出4和6
 */
public class NumAppearOnce {

    @Test
    public void test() {
        System.out.println(Arrays.toString(getNumAppearOnce(new int[]{2,4,3,6,3,2,5,5,4,10})));
    }

    /**
     * 思路比较数学
     * 两个相等的元素异或结果为0，0与任意数字x异或的结果都为x，
     * 接下来，需要将数组分成两组，每组内只包含一个不重复数字，即可在这两个子数组中分别找到两个不重复数字
     *
     * 当数组中有两个元素不重复时，最终是这两个数字异或的结果，且不为0，这个数字在二进制表示中至少有一位是1
     * 以此为标准，再次遍历数组，将该标志位为1的分为一组，不为1的分为一组，对两个子数组进行异或，即可得到两个不重复数字
     *
     */
    private int[] getNumAppearOnce(int[] nums) {
        int[] res = new int[2];
        int diff = 0;
        for (int n: nums) {
            diff ^= n;
        }
        diff &= -diff; // 可以得到最右侧为1的位，比如14和-14，二进制是1110和11110010，与操作后是10
        for (int n: nums) {
            // 一个数与最右侧为1的位进行与操作，最终只能是 0 或者 其他数字，由此将数组分成两拨
            if ((n & diff) == 0) {
                res[0] ^= n;
            } else {
                res[1] ^= n;
            }
        }
        return res;
    }
}

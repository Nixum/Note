package bit;

import org.junit.Test;

import java.util.Arrays;

/**
 * 一个整型数组里除了两个数字只出现一次之外，
 * 其他的数字都出现了两次，找出这两个数。
 * 输入： 1 1 2 2 3 4 4 5 6 6
 * 输出：3 5
 */
public class NumberAppearOnce {

    @Test
    public void test() {
        int[] array = new int[]{1, 1, 2, 2, 3, 4, 4, 5, 6, 6};
        int[] res = new int[2];
        findAppearOnceNumber(array, res);
        System.out.println(Arrays.toString(res));
    }

    /**
     * 两个相同的数异或=0，因此第一遍之后会得到那两个不同的数的异或值
     * diff &= -diff 得到出 diff 最右侧不为 0 的位，
     * 也就是不存在重复的两个元素在位级表示上最右侧不同的那一位, 利用这一位来区分, 再异或一遍得到
     *
     */
    public void findAppearOnceNumber(int[] nums, int[] res) {
        if (nums == null || nums.length <= 0 || res == null || res.length != 2)
            return ;
        int diff = 0;
        for (int n : nums) {
            diff ^= n;
        }
        diff &= -diff;
        for (int n : nums) {
            if ((diff & n) == 0) {
                res[0] ^= n;
                System.out.println(n + " ad " + res[0]);
            } else {
                res[1] ^= n;
                System.out.println(n + " and " + res[1]);
            }
        }
    }
}

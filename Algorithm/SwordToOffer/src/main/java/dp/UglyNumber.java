package dp;

import org.junit.Test;

/**
 * 把只包含因子 2、3 和 5 的数称作丑数，如果一个数能被2/3/5整除，再连续被整除，最后得到的结果是1，就是丑数。
 * 例如 6、8 都是丑数，但 14 不是，因为它包含因子 7。
 * 习惯上我们把 1 当做是第一个丑数。求按从小到大的顺序的第 N 个丑数。
 *
 * 前20个数的丑数：1，2，3，4，5，6，8，9，10，12，15，16，18，20
 */
public class UglyNumber {

    @Test
    public void test() {
        System.out.println(getUglyNumber(8));
        System.out.println(getUglyNumber2(8));
        System.out.println(getUglyNumber(1500));
        System.out.println(getUglyNumber2(1500));
    }

    /**
     * 暴力法：从1开始，计算每一个丑数，直到到达第n个
     */
    private int getUglyNumber2(int n) {
        int num = 0;
        int m = 1;
        while(m <= n) {
            num++;
            if (isUglyNumber(num)) {
                m++;
            }
        }
        return num;
    }

    private boolean isUglyNumber(int num) {
        while(num % 2 == 0) {
            num /= 2;
        }
        while(num % 3 == 0) {
            num /= 3;
        }
        while(num % 5 == 0) {
            num /= 5;
        }
        return num == 1;
    }

    /**
     * 暴力算法是对每一个数进行判断，对于非丑数也仍然需要判断，太过费劲，
     * 如果可以一开始就生成丑数的话，就会减少很多没必要的判断了
     * 思路：找到2，3，5累乘规律，并将结果从小到大排列，
     * 1，2，3，4，5，6，8，9，10，12，15，16，18，20
     * 1，【1】*2，【1】*3，【2】*2，【1】*5，【2】*3，【【2】*2】*2，【3】*3，【2】*5 ...
     * 每一个丑数都是前面的某个丑数乘2、3或5：遍历每个生成的丑数，分别乘以2、3、5，取出最小的那个，作为当前丑数的下一个丑数
     * 对于每轮乘以2/3/5，都是从1开始，
     * 比如有 底数1，乘以2，3，5得到2，3，5，取走2，此时有丑数1，2
     *      但是3，5也是丑数，下一轮乘2，3，5的时候，3，5还在，2的底数变成了当前底数(1)的下一个丑数(2)，乘以2，
     * 得到 2*2，3，5；取走3，此时有丑数1，2，3
     *      但是2*2，5也是丑数，下轮乘2，3，5的时候，2*2，5还在，3的底数变成了当前底数(1)的下一个丑数(2)，乘以3，
     * 得到2*2，2*3，5；取走4，此时有丑数1，2，3，4
     *      但2*3，5也是丑数，下轮乘2，3，5的时候，2*3，5还在，2的底数变成了当前底数(2)的下一个丑数(3)，乘以2，
     * 得到3*2，2*3，5；取走5，此时有丑数1，2，3，4，5
     */
    private int getUglyNumber(int n) {
        if (n < 0) {
            return -1;
        }
        if (n <= 6 && n >= 1) {
            return n;
        }
        int[] res = new int[n];
        res[0] = 1;
        int i2 = 0, i3 = 0, i5 = 0; // 一开始2，3，5当前丑数都是res[0]，即=1
        for (int i = 1; i < n; i++) {
            int next2 = res[i2] * 2;
            int next3 = res[i3] * 3;
            int next5 = res[i5] * 5;
            // 取出最小的那个
            res[i] = Math.min(next2, Math.min(next3, next5));
            // 将最小的那个丑数的底数变成底数的下一个丑数，进行下一轮乘法计算
            if (res[i] == next2) {
                i2++;
            }
            if (res[i] == next3) {
                i3++;
            }
            if (res[i] == next5) {
                i5++;
            }
        }
        return res[n - 1];
    }
}

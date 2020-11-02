package bit;

import org.junit.Test;

/**
 * 计数 从 1 到 n 整数中 1 出现的次数
 */
public class NumOfOneBetween1AndN {

    @Test
    public void test() {
        System.out.println(count1Between1AndN(13));
        System.out.println(count1Between1AndN1(13));
    }

    /**
     * 暴力解法：对于每个数， %10 判断个位数是否是 1，/10削去最后一位，得到的数再判断1的个数
     */
    private int count1Between1AndN1(int n) {
        if (n <= 0) {
            return 0;
        }
        int cnt = 0;
        for (int i = 1; i <= n; i++) {
            cnt += numberOfOne(i);
        }
        return cnt;
    }

    private int numberOfOne(int n) {
        int cnt = 0;
        while(n != 0) {
            if (n % 10 == 1) {
                cnt++;
            }
            n /= 10;
        }
        return cnt;
    }

    /**
     * 思路：下面这种思路纯数学分析了。我只能说I am stupid。
     * 链接：https://leetcode-cn.com/problems/number-of-digit-one/solution/xiang-xi-tong-su-de-si-lu-fen-xi-duo-jie-fa-by-50/
     */
    private int count1Between1AndN(int n) {
        if (n <= 0) {
            return 0;
        }
        int cnt = 0;
        for (int i = 1; i <= n; i *= 10) {
            int a = n / i; // 削去最后一位
            int b = n % i; // 取出个位数
            cnt += (a + 8) / 10 * i + (a % 10 == 1 ? b + 1 : 0);
        }
        return cnt;
    }
}

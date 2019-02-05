package recursion.dp;

import org.junit.Test;

/**
 * 递归式 n=0, return 0
 *       n=1, return 1
 *       n>1, return f(n-1) + f(n-2)
 * 类似的题目：一只青蛙一次可以跳上 1 级台阶或 2 级台阶，问上n级台阶有共有多少种跳法
 *          一只青蛙一次可以跳上 1 级台阶或 2 级台阶或 n 级台阶，问上n级台阶有共有多少种跳法 2^(n-1)
 *          8个2x1的矩形，覆盖2x8的矩阵有多少种方法
 */
public class Fibonacci {

    @Test
    public void test() {
        System.out.println(calculateByLoop(40));
    }

    /**
     * 一般递归法
     */
    public int calculate (int n) {
        if (n == 1) {
            return 1;
        }
        if (n <= 0) {
            return 0;
        }
        return calculate(n - 1) + calculate(n - 2);
    }

    /**
     * 上面那种方法会导致重复计算
     * 动态规划，将每次计算的值都存起来
     */
    public int calculateByDP(int n){
        if (n <= 0) {
            return 0;
        }
        if (n == 1) {
            return 1;
        }
        int[] res = new int[n + 1];
        res[1] = 1;
        res[2] = 1;
        for(int i = 3; i <= n; i++) {
            res[i] = res[i - 1] + res[i - 2];
        }
        return res[n];
    }

    /**
     * 直接迭代，不存每次计算的值
     */
    public int calculateByLoop(int n) {
        if (n <= 0) {
            return 0;
        }
        if (n == 1) {
            return 1;
        }
        int n_3 = 2, n_1 = 1, n_2 = 1;      // n_3 = n_1 + n_2 ---> f(3) = f(1) + f(2);
        for(int i = 3; i <= n; i++) {
            n_3 = n_1 + n_2;
            n_1 = n_2;
            n_2 = n_3;
        }
        return n_3;
    }
}

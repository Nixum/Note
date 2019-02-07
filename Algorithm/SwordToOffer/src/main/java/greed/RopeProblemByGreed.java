package greed;

import org.junit.Test;

/**
 * 剪绳子问题，贪婪算法就比较简单一点，
 * 不过要先知道当 n >= 5 时，3(n - 3) - 2(n - 2) = n - 5 >= 0。
 * 因此把长度大于 5 的绳子切成两段，令其中一段长度为 3 可以使得两段的乘积最大
 * 但是当剩下的绳子是4时，得分解成2x2才是最优的才行
 * 这需要数学证明，才知道
 */
public class RopeProblemByGreed {
    @Test
    public void test() {
        System.out.println(cut(8));
    }

    public int cut(int n) {
        if (n < 2)
            return 0;
        if (n == 2)
            return 1;
        if (n == 3)
            return 2;
        int timesOfThree = n / 3;
        if (n - timesOfThree * 3 == 1)
            timesOfThree--;
        int timesOfTwo = (n - timesOfThree * 3) / 2;
        return (int) (Math.pow(3, timesOfThree)) * (int) (Math.pow(2, timesOfTwo));
    }
}

package dp;

import org.junit.Test;

import java.util.Arrays;

/**
 * 给你一根长度为n的绳子，把绳子剪成m段（m,n都是整数，且均大于1），
 * 每段绳子d的长度记为k[0],k[1]...k[m]。请问k[0]k[1]...k[m]可能的最大乘积是多少？
 * 例如，绳子长度为8，当把它剪成2、3、3时，乘积最大，为18
 * input:8
 * output:18    (8 = 2 + 3 + 3)
 * input:2
 * output:1     (2 = 1 + 1)
 * input:3
 * output:2     (3 = 2 + 1)
 */
public class RopeProblemByDp {

    @Test
    public void test() {
        System.out.println(cut(8));
    }

    /**
     * 一根绳子f(n),剪一刀,会变成f(i)*f(n-i),0<i<n，有从i=1开始剪剪到i=n-1的可能，分成两半，
     * 分成的两半中，都需要各自选出最优的出来相乘，比如，n=3时，可以取 1x2 ，而2又可以1x1，
     * 最终 1x2>1x1x1,即 f(3) = max(1x2, 1xf(3-1)=1xf(2)=1)
     * 这里主要是要理解Math.max(dp[i], Math.max(j * (i - j), dp[j] * (i - j)))
     */
    public int cut(int n) {
        int[] dp = new int[n + 1];
        dp[1] = 1;
        for (int i = 2; i <= n; i++)
            for (int j = 1; j < i; j++)
                dp[i] = Math.max(dp[i], Math.max(j * (i - j), dp[j] * (i - j)));
        return dp[n];
    }

}

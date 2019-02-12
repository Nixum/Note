package recursion;

import org.junit.Test;

/**
 * 求数值的整数方根
 * 乍一看好像很容易，但是要注意特殊值，0和负数为指数时，就不能普通的使用循环去乘了
 * 因此写的时候要考虑这几点
 * 当底数或指数为0的时候、底数或指数为负数的时候、考虑如何乘比较高效(减少循环次数)
 */
public class NumPower {

    @Test
    public void test() {
        double r = power(2, 10);
        System.out.println(r);
    }

    /**
     * 指数为0，返回1
     * 指数为负数，采用一个布尔值来判定，返回倒数
     * 指数为1，返回底数
     * 其他情况
     * result = a^(n/2) * a^(n/2)，n为偶数
     *        = a^((n-1)/2) * a^((n-1)/2) * a，n为奇数
     */
    public double power(double base, int exponent) {
        if (exponent == 0)
            return 1;
        if (exponent == 1)
            return base;
        boolean isNegative = false;
        if (exponent < 0) {
            isNegative = true;
            exponent = -exponent;
        }
        double result = power(base, exponent / 2);
        result = result * result;
        if (exponent % 2 == 1)
            result = result * base;

        return isNegative ? 1 / result : result;
    }
}

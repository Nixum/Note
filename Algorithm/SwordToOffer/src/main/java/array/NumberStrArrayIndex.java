package array;

import org.junit.Test;

/**
 * 数字以 0123456789101112131415... 的格式序列化到一个字符串中，求这个字符串的第 index 位
 * 比如index=5，第5位就是5，第13位是1，第19位是4，index下标从0开始
 */
public class NumberStrArrayIndex {

    @Test
    public void test() {
        System.out.println(getDigitAtIndex1(503));
        System.out.println(getDigitAtIndex1(10));
        System.out.println(getDigitAtIndex1(1));
    }

    /**
     * 暴力的做法是：从0开始枚举每个数，计算其位数，进行累加，当累加的位数比index大时，取该数进行位数获取。
     * 观察可得，个位数是1位，一共有10个，有10位；十位数是2位，一共有90个，有180位，百位数是3位，共有900个，有2700位
     */
    private int getDigitAtIndex1(int index) {
        if (index < 0) {
            return -1;
        }
        int place = 1; // 位数，1为各位，2为十位
        while(true) {
            int digitSum = getDigitSumByPlace(place);
            int totalSum = digitSum * place;
            if (totalSum > index) {
                return getDigitIndex(index, place);
            }
            index -= totalSum;
            place++;
        }
    }

    // 获取位数的总个数
    private int getDigitSumByPlace(int place) {
        if (place == 1) {
            return 10;
        }
        return (int) Math.pow(10, place - 1) * 9;
    }

    // 找到index在place位数的第几个数的数字，
    // 比如index = 503，当place=3时，加起来就有 1000 > (313 = 503 - 10 - 180(个位和十位的位数和)),
    // 说明当到达百位时，就超出很多了，此时index = 313，place = 3，即从100开始，找到第313位的数字
    private int getDigitIndex(int index, int place) {
        int beginNum = 0;
        if (place != 1) {
            beginNum = (int) Math.pow(10, place - 1); // place位数的起始位，比如place=3，百位，起始位是100
        }
        int shiftNum = index / place; // 偏移量，313 / 3 = 104个，即从数字100开始，到达数字204时，包含了index的位置
        String targetNum = (beginNum + shiftNum) + "";
        int bit = index % place; // 313 % 3 = 1，即在数字204的第二位就是index的位置，0为起始位
        return targetNum.charAt(bit) - '0';
    }
}

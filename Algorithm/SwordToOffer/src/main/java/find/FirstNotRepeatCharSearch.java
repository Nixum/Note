package find;

import org.junit.Test;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;

/**
 * 请实现一个函数用来找出字符流中第一个只出现一次的字符。
 * 例如，当从字符流中只读出前两个字符 "go" 时，第一个只出现一次的字符是 "g"。
 * 当从该字符流中读出前六个字符 "google" 时，第一个只出现一次的字符是 "l"。
 */
public class FirstNotRepeatCharSearch {

    @Test
    public void test() {
        char[] charList = new char[] {'g', 'o', 'o', 'g', 'l', 'e'};
        System.out.println(searchFirstNotRepeatChar(charList));
        charList = new char[] {'g', 'o'};
        System.out.println(searchFirstNotRepeatChar(charList));
    }

    /**
     * 利用队列先进先出 + 字符会有asic码对应的数字为下标记录字符出现次数 的特点
     * 遍历字符数组，对于获取的元素，进行计数，队列中只保留第一个次数为0的字符，其余的remove即可
     */
    private char searchFirstNotRepeatChar(char[] charList) {
        int[] cnts = new int[256];
        Queue<Character> queue = new LinkedList<>();
        for (char c : charList) {
            cnts[c]++;
            queue.add(c);
            while (!queue.isEmpty() && cnts[queue.peek()] > 1) {
                queue.poll(); // = remove
            }
        }
        return queue.isEmpty() ? '#' : queue.peek();
    }

}

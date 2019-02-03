import org.junit.Test;

import java.util.*;

/**
 * 长度为n的数组的所有数字都在0~n-1范围内。数组某些数字是重复的，但不知道有几个数字重复，
 * 也不知道数字重复了几次，请找出数组中任意一个重复的数字(题目只需要找到重复数字即可)
 * input：7 2, 3, 1, 0, 2, 5, 3
 * output:2, 3
 */
public class RepeatNumber {

    @Test
    public void test(){
//        List<Integer> resultList1 = getRepeatNumber(7, new int[]{2, 3, 1, 0, 2, 5, 3});
//        System.out.println(resultList1);

        List<Integer> resultList = new ArrayList<>();
        boolean flag = getRepeatNumberBest(7, new int[]{2, 3, 1, 0, 2, 5, 3}, resultList);
        System.out.println(resultList);

    }

    /**
     * 1.第一遍遍历得到各个数字的出现的次数，存入map，记录最大的出现次数max
     * 2.遍历map，根据max找到该数字
     */
    public List<Integer> getRepeatNumber(int n, int[] array){
        int max = 0;
        List<Integer> resultList = new ArrayList<>();
        if (array == null || n <= 0)
            return resultList;
        Map<Integer,Integer> tempMap = new HashMap<>();
        for (int i = 0; i < n; i++) {
            Integer curr = tempMap.get(array[i]);
            if (curr == null) {
                tempMap.put(array[i], 1);
            } else {
                tempMap.put(array[i], curr + 1);
                if (max < curr + 1) {
                    max = curr + 1;
                }
            }
        }
        Set<Integer> keySet = tempMap.keySet();
        Iterator<Integer> keyIterator = keySet.iterator();
        while (keyIterator.hasNext()) {
            Integer key = keyIterator.next();
//            找到重复最多的数字
//            if (tempMap.get(key) == max) {
//                resultList.add(key);
//            }
//            只找到重复数字
            if (tempMap.get(key) > 1) {
                resultList.add(key);
            }
        }
        return resultList;
    }

    /**
     * 这种方法只能找到第一个重复数字
     * 1.根据 长度为n的数组的所有数字都在0~n-1范围内 这个特性，比较数组下标和对应的值
     * 2.取数组第i个，数组的值m和数组的下标i是否一致，如果是，取i+1继续判断，如果不是，取下标为m的值，判断是否与m一致
     * 如果一致，则找到一个重复数字，如果不是，交换第i个数字和第m个数字，之后继续重复比较，交换，直至结束
     */
    public boolean getRepeatNumberBest(int n, int[] array,List<Integer> resultList){
        if (array == null || n <= 0)
            return false;
        for (int i = 0; i < n; i++) {
            while (i != array[i]) {
                if (array[array[i]] == array[i]) {
                    resultList.add(array[i]);
                    return true;
                }
                int temp = array[i];
                array[i] = array[temp];
                array[temp] = temp;
            }
        }
        return false;
    }
}
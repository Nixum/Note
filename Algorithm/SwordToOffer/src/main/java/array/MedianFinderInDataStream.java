package array;

import java.util.PriorityQueue;

/**
 * leetcode 295
 * 在一个很大的有序数据流列表中找到中位数
 * 如果列表长度是偶数，则中位数是中间两个数的平均值
 */
public class MedianFinderInDataStream {

    /**
     * 利用两个堆，一个大根堆，一个小根堆来保证有序，
     * 需要保证两个堆的大小差值为1，且小根堆的堆顶要大于大根堆的堆顶元素
     * 中位数通过两个堆的堆顶元素和两个堆的长度来计算得到
     */
    PriorityQueue<Integer> small;
    PriorityQueue<Integer> large;

    public MedianFinderInDataStream() {
        large = new PriorityQueue<>();
        small = new PriorityQueue<>((a, b) -> b-a);
    }

    public double findMedian() {
        if (large.size() < small.size()) {
            return small.peek();
        } else if (large.size() > small.size()) {
            return large.peek();
        }
        return (large.peek() + small.peek()) / 2.0;
    }

    /**
     * 因为要保证有序，不能简单的往长度少的那个堆加元素，
     * 否则有可能出现大根堆的堆顶元素大于小根堆的堆顶元素
     * 时间复杂度为O(logN)
     */
    public void addNum(int n) {
        if (large.size() <= small.size()) {
            small.offer(n);
            large.offer(small.poll());
        } else {
            large.offer(n);
            small.offer(large.poll());
        }
    }

}

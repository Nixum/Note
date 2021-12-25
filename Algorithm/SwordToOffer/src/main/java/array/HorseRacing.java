package array;

import java.util.Arrays;
import java.util.PriorityQueue;

/** 类似leetcode 870
 * 有两个数组nums1和nums2，现要重新组织nums中元素的位置，使得nums1的优势最大化
 * 即 nums1 中每个下标对应的元素 大于 nums中元素个数最多，比如：
 * 输入：nums1为 12，24，8，32；nums2为 13，25，32，11
 * 输出：nums1为 24，32，8，12
 */
public class HorseRacing {

    /**
     * 思路：类似田忌赛马，对两数组进行排序，拿nums2中的最大的跟nums1中最小的对应即可
     * 要注意的是nums2的顺序不能乱，计算结果依赖nums2中的顺序
     * 所以需要一种结构，来保存nums2，使得可以让nums2中的可以降序，且能记录元素的原下标
     */
    public int[] racing(int[] nums1, int[] nums2) {
        int[] res = new int[nums1.length];
        // 升序排列
        Arrays.sort(nums1);
        // 降序排列
        PriorityQueue<int[]> pq = new PriorityQueue<>((int[] p1, int[] p2) -> p2[1] - p1[1]);
        for (int i = 0; i < nums2.length; i++) {
            pq.offer(new int[]{i, nums2[i]});
        }

        int left = 0, right = nums1.length - 1;
        while (!pq.isEmpty()) {
            int[] p = pq.poll();
            int nums2Index = p[0], nums2Val = p[1];
            if (nums2Val > nums1[right]) {
                res[nums2Index] = nums1[left];
                left++;
            } else {
                res[nums2Index] = nums1[right];
                right--;
            }
        }
        return res;
    }
}

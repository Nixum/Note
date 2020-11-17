package array;

import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

/**
 * 在有序数组中找出两个数，使得和为给定的数 S。如果有多对数字的和等于 S，输出两个数的乘积最小的。
 */
public class TwoNumEqualNum {

    @Test
    public void test() {
        System.out.println(getTwoNumEqualNum(new int[]{1,2,4,7,11,15}, 15));
    }

    /**
     * 解决方案：
     * 首先，由于是有序数组，从头到尾遍历，第一组获取的满足target的值一定是乘积最小的！！
     * 1.先遍历使用hashMap存数组中的元素，key为nums[i]，value为i，
     *   再次遍历数组，在map中找target-num[i]的数的下标
     * 2.双指针法，头尾两个指针向中间靠拢，将头尾指针指向的值进行相加，如果大于target，尾指针-1，反之头指针+1
     *   直到找到和为目标值的两个元素
     */
    private List<Integer> getTwoNumEqualNum(int[] nums, int target) {
        List<Integer> res = new ArrayList<>();
        int i = 0, j = nums.length - 1;
        while (i < j) {
            int sum = nums[i] + nums[j];
            if (sum == target) {
                res.add(nums[i]);
                res.add(nums[j]);
                break;
            }
            if (sum < target) {
                i++;
            } else {
                j--;
            }
        }
        return res;
    }
}

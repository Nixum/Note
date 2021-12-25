package array;

/**
 * leetcode 27
 * 给定数组nums和一个值val，原地移除数组中所有等于val的值，并返回移除后的新数组和新长度
 * 空间复杂度要求为 O(1)，元素的顺序可以改变，无需考虑数组中超出新长度后面的数组
 * 给定 nums = [3,2,2,3]，val = 3，返回长度2，nums前两个元素为[2,2]
 * 给定 nums = [0,1,2,2,3,0,4,2]，val = 2，返回长度5，nums前5个元素为[0,1,3,0,4]
 */
public class TargetNumRemove {

    public int RemoveTargetNum(int[] nums, int val) {
        if (nums == null || nums.length <= 0) {
            return 0;
        }
        int slow = 0, fast = 0;
        while (fast < nums.length) {
            if (nums[fast] != val) {
                nums[slow] = nums[fast];
                slow ++;
            }
            fast++;
        }
        return slow;
    }
}

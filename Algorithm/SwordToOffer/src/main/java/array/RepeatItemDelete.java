package array;

/** leetcode 26, 83
 * 给定排序数组 nums = [1, 1, 2]，返回新长度2，且原数组nums的前两个元素被修改为1, 2
 * 不需要考虑数组中超出新长度后面的元素
 * 比如
 * 给定 nums = [0,0,1,1,1,2,2,3,3,4]，返回5，且原数组nums的前5个元素被修改为 0，1，2，3，4
 * 必须原地修改数组，空间复杂度为 O(1)
 */
public class RepeatItemDelete {

    public int removeRepeatItem(int[] nums) {
        if (nums == null || nums.length <= 0) {
            return 0;
        }
        int slow = 0, fast = 0;
        while (fast < nums.length) {
            if (nums[fast] != nums[slow]) {
                slow++;
                nums[slow] = nums[fast];
            }
            fast++;
        }
        return slow + 1;
    }

}

package array;

/**
 * leetcode 283
 * 输入一个数组，原地修改，将数组中所有值为0的元素移到数组末尾
 * 比如输入 nums = [0, 1, 4, 0, 2]，会把nums修改为 [1, 4, 2, 0, 0]
 */
public class MoveZeroNumToLast {

    public void moveZero(int[] nums) {
        if (nums == null || nums.length <= 0) {
            return;
        }
        int slow = 0, fast = 0;
        while (fast < nums.length) {
            if (nums[fast] != 0) {
                nums[slow] = nums[fast];
                slow++;
            }
            fast++;
        }
        for (int i = slow; i < nums.length; i++) {
            nums[i] = 0;
        }
    }
}

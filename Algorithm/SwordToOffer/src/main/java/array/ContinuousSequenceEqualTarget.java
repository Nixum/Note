package array;

import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

/**
 * 输出所有和为 S 的连续正数序列。例如：
 * 在0~100的序列中，和为100的连续序列有
 * [9, 10, 11, 12, 13, 14, 15, 16]
 * [18, 19, 20, 21, 22]
 */
public class ContinuousSequenceEqualTarget {

    @Test
    public void test() {
        System.out.println(getContinuousSequenceEqualTarget(100));
    }

    /**
     * 思路：滑动窗口，终止条件是窗口的左边界超过和的一半，因为之后相加不可能=目标值了
     */
    private List<List<Integer>> getContinuousSequenceEqualTarget(int target) {
        List<List<Integer>> res = new ArrayList<>();
        int l = 0, r = 0;
        int tmpTarget = 0;
        while (l <= target / 2) {
            if (tmpTarget < target) {
                tmpTarget += r;
                r++;
            } else if (tmpTarget > target) {
                tmpTarget -= l;
                l++;
            } else {
                List<Integer> res1 = new ArrayList<>();
                for (int i = l; i < r; i++) {
                    res1.add(i);
                }
                res.add(res1);
                tmpTarget -= l;
                l++;
            }
        }
        return res;
    }
}

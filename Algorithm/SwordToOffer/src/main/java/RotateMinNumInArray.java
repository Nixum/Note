import org.junit.Test;

/**
 * 选择：把一个数组最开始的若干个元素搬到数组末尾
 * 输入一个递增排序数组的一个旋转，输出旋转数组的最小元素
 * input: {4, 5, 6, 1, 2, 3}
 * output: 1
 */
public class RotateMinNumInArray {

    @Test
    public void test() {
        System.out.println(findInRotateDuplicateArray(new int[]{2, 3, 4, 5, 6, 1}));
        System.out.println(findInRotateNoDuplicateArray(new int[]{1,1,1,0,1}));
    }

    /**
     * 当数组不允许重复时，递增数组的旋转
     * 二分查找，在最左端的值大于最右端的值时，每次找到中间值都与最左端进行比较，
     * 如果大于等于最左端的值，说明最小值在右边
     * 如果小于或等于最右端的值，说明最小值在左边
     * 最终找到左端是最大值，右端是最小值的情况，注意要判断，如果没判断左右端是否相邻，会导致无限循环
     */
    public int findInRotateDuplicateArray(int[] array) {
        if (array == null || array.length <= 0) {
            return -1;
        }
        int start = 0;
        int end = array.length - 1;
        int mid = start;
        while (array[start] >= array[end]) {
            if ((end - start) == 1) {
                mid = end;
                break;
            }
            mid = (start + end) / 2;
            if (array[mid] >= array[start]) {
                start = mid;
            } else if (array[mid] <= array[end]) {
                end = mid;
            }
        }
        return array[mid];
    }

    /**
     * 允许重复的情况下，例如{1,1,1,0,1}，就没办法用上面的方法去找了
     * 因为可能出现array[start] == array[mid] == array[end]，此时需要顺序查找了
     */
    public int findInRotateNoDuplicateArray(int[] array) {
        if (array == null || array.length <= 0) {
            return -1;
        }
        int start = 0;
        int end = array.length - 1;
        int mid = start;
        while (array[start] >= array[end]) {
            if ((end - start) == 1) {
                mid = end;
                break;
            }
            mid = (start + end) / 2;
            if (array[mid] >= array[start]) {
                start = mid;
            } else if (array[mid] <= array[end]) {
                end = mid;
            } else if(array[mid] == array[start] && array[mid] == array[end]) {
                // 顺序查找
                findInArray(array, start, end);
            }
        }
        return array[mid];
    }

    private int findInArray(int[] array, int start, int end) {
        for (int i = start; i <= end; i++) {
            if (array[i] > array[i + 1])
                return array[i + 1];
        }
        return array[start];
    }
}

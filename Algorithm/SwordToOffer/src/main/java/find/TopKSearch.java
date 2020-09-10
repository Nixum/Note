package find;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;

/**
 * 在数组或海里数据中找出最大/小的K个数
 *
 */
public class TopKSearch {

  @Test
  public void testTopKSearch() {
    int[] arr = new int[]{1,5,7,0,4,2,6,8,3,0};
    System.out.println(Arrays.toString(topKSearch(arr, 3)));

    int[] arr2 = new int[]{1,5,7,0,4,2,6,8,3,0};
    System.out.println(Arrays.toString(topKSearch2(arr2, 4)));
  }

  /**
   * 使用大根堆来找出最小的K个数
   * 思路：先从数组中取出k个数，初始化为大根堆，此时根节点为前k个数最大的
   * 从原始数组中的k+1个开始遍历，与大根堆的根节点进行比较，如果比根节点的值小，则与根节点交换，
   * 在调整成大根堆，依次直到数组遍历完成，即可获得最小的k个数
   * 时间复杂度为nlog2k，空间复杂度为k
   */
  public int[] topKSearch(int[] arr, int k) {
    if (arr == null || arr.length <= 0 || k >= arr.length || k <= 0) {
      return new int[]{};
    }
    for (int i = k / 2 - 1; i >= 0; i--) {
      adjust(arr, i, k);
    }
    for (int i = k; i < arr.length; i++) {
      if (arr[i] < arr[0]) {
        int tmp = arr[i];
        arr[i] = arr[0];
        arr[0] = tmp;
      }
      adjust(arr, 0, k);
    }
    return Arrays.copyOf(arr, k);
  }

  private void adjust(int[] arr, int rootIndex, int len) {
    int tmp = arr[rootIndex];
    for (int i = 2 * rootIndex; i < len; i++) {
      if (i + 1 < len && arr[i] < arr[i + 1]) {
        i++;
      }
      if (tmp < arr[i]) {
        arr[rootIndex] = arr[i];
        rootIndex = i;
      } else {
        break;
      }
    }
    arr[rootIndex] = tmp;
  }

  /**
   * 利用快排的切分思想，一次递归可以获取到以基数值为中间节点，左边的数都比它小，右边的数都比它大
   * 所以，只需要找到k个数的k-1的索引与基数值索引的关系，比如
   * 一次递归后，基数的索引为k-1，此时基数左边的k个数就是要找的
   * 基数的索引>k-1，说明这k个数在基数的左半部分，在对基数的左边部分在做一次递归，直到找到基数索引=k-1
   * 基数的索引<k-1，说明这k个数在基数的右半部分，在对基数的右边部分在做一次递归，直到找到基数索引=k-1
   * 时间复杂度为n，最坏n^2，空间复杂度为log2n
   */
  public int[] topKSearch2(int[] arr, int k) {
    if (arr == null || arr.length <= 0 || k >= arr.length || k <= 0) {
      return new int[]{};
    }
    int l = 0;
    int r = arr.length - 1;
    while (l < r) {
      int mid = partition(arr, l, r);
      if (mid == k) {
        break;
      }
      if (mid > k) { // 说明在左边缩小范围
        r = mid - 1;
      } else {
        l = mid + 1; // 说明在右边缩小范围
      }
    }
    return Arrays.copyOf(arr, k);
  }

  private int partition(int[] arr, int l, int r) {
    int tmp = arr[l];
    int ll = l, rr = r;
    while (ll < rr) {
      while (ll < rr && arr[rr] >= tmp) {
        rr--;
      }
      arr[ll] = arr[rr];
      while (ll < rr && arr[ll] < tmp) {
        ll++;
      }
      arr[rr] = arr[ll];
    }
    arr[ll] = tmp;
    return ll;
  }
}

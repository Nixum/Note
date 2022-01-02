package model;

import java.util.*;

/**
 * 设计一个数据结构，其在insert，remove，getRandom方法执行的时时间复杂度是O(1)
 * getRandom方法会随机返回现有集合中的一项，每个元素以相同的概率被返回
 * RandomizedSet randomizedSet = new RandomizedSet();
 * randomizedSet.insert(1); // Inserts 1 to the set. Returns true as 1 was inserted successfully.
 * randomizedSet.remove(2); // Returns false as 2 does not exist in the set.
 * randomizedSet.insert(2); // Inserts 2 to the set, returns true. Set now contains [1,2].
 * randomizedSet.getRandom(); // getRandom() should return either 1 or 2 randomly.
 * randomizedSet.remove(1); // Removes 1 from the set, returns true. Set now contains [2].
 * randomizedSet.insert(2); // 2 was already in the set, so return false.
 * randomizedSet.getRandom(); // Since 2 is the only number in the set, getRandom() will always return 2.
 */
public class RandomizedSet {

    /**
     * 因为getRandom方法的存在，无法直接使用HashSet或LinkedHashSet
     * 底层利用数组和Map，插入是往数组尾部插入，
     * 由于无需保证有序性，删除时将要删除的元素与最后一个元素交换，然后删除最后一个
     * 这样的话，就能保证random方法可以等概率，且时间复杂度为O(1)
     */

    List<Integer> nums;
    // key为插入的值，val为对应的下标索引
    Map<Integer, Integer> val2Index;

    public RandomizedSet() {
        this.nums = new ArrayList<>();
        this.val2Index = new HashMap<>();
    }

    public boolean insert(int val) {
        if (val2Index.containsKey(val)) {
            return false;
        }
        val2Index.put(val, nums.size());
        nums.add(val);
        return true;
    }

    public boolean remove(int val) {
        if (!val2Index.containsKey(val)) {
            return false;
        }
        int index = val2Index.get(val);
        int lastIndex = nums.size() - 1;
        int lastVal = nums.get(lastIndex);
        val2Index.put(lastVal, index);

        // 最后一个值与目标值交换
        int tmp = nums.get(lastIndex);
        nums.set(lastIndex, val);
        nums.set(index, tmp);

        nums.remove(lastIndex);
        val2Index.remove(val);
        return true;
    }

    public int getRandom() {
        Random n = new Random();
        return nums.get(n.nextInt(this.nums.size()));
    }

}

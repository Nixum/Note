package model;

import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;

/**
 * 频次最少使用
 * 设定容量，每次get key会修改使用次数和使用时间，当满容量时，移除次数最少的那个
 * 如果有多个key的使用次数一样，则移除使用时间最旧的那个
 */
public class LFUCache {

    Map<String, String> keyValMap;
    Map<String, Integer> key2FreqMap;
    // 用linkHashSet来模拟使用时间
    Map<Integer, LinkedHashSet<String>> freq2KeysMap;
    int minFreq;
    int cap;

    public LFUCache(int cap) {
        keyValMap = new HashMap<>();
        key2FreqMap = new HashMap<>();
        freq2KeysMap = new HashMap<>();
        this.cap = cap;
        this.minFreq = 0;
    }

    public String get(String key) {
        if (!keyValMap.containsKey(key)) {
            return "";
        }
        increaseFreq(key);
        return keyValMap.get(key);
    }

    public void put(String key, String val) {
        if (this.cap <= 0) {
            return;
        }
        if (keyValMap.containsKey(key)) {
            keyValMap.put(key, val);
            increaseFreq(key);
            return;
        }
        if (this.cap <= keyValMap.size()) {
            removeMinFreqKey();
        }
        keyValMap.put(key, val);
        key2FreqMap.put(key, 1);
        freq2KeysMap.putIfAbsent(1, new LinkedHashSet<>());
        freq2KeysMap.get(1).add(key);
        this.minFreq = 1;
    }

    void increaseFreq(String key) {
        int freq = key2FreqMap.get(key);
        key2FreqMap.put(key, freq+1);
        freq2KeysMap.get(freq).remove(key);
        freq2KeysMap.putIfAbsent(freq + 1, new LinkedHashSet<>());
        freq2KeysMap.get(freq + 1).add(key);
        if (freq2KeysMap.get(freq).isEmpty()) {
            freq2KeysMap.remove(freq);
            if (freq == this.minFreq) {
                this.minFreq++;
            }
        }
    }

    void removeMinFreqKey() {
        LinkedHashSet<String> keys = freq2KeysMap.get(this.minFreq);
        String delKey = keys.iterator().next();
        keys.remove(delKey);
        if (keys.isEmpty()) {
            freq2KeysMap.remove(this.minFreq);
            // 这里无需更新 minFreq 的值，因为该方法是在插入新key时使用，此时minFreq一定是1
        }
        keyValMap.remove(delKey);
        key2FreqMap.remove(delKey);
    }
}

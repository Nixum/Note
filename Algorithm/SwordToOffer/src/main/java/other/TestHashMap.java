package other;

import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

public class TestHashMap {

    @Test
    public void test() {
        Map<String, String> map = new HashMap<>();
        System.out.println();
        map.put("key", "value");
        int h;
        System.out.println((h = "key111".hashCode()) ^ (h >>> 16));
    }
}

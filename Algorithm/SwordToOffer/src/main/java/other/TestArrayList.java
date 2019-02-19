package other;

import org.junit.Test;

import java.util.*;

public class TestArrayList {

    @Test
    public void testClassEqual() {
        List<Integer> list = new ArrayList<>();
        Object[] elementData = list.toArray();
        if (elementData.getClass() == Object[].class) {
            System.out.println("yes");
        }
        List<String> asList = Arrays.asList("aaa","bbb");
        System.out.println(asList.toArray().getClass());


    }

    @Test
    public void testSize() {
        List<String> l1 = new ArrayList<>(10);
        System.out.println(l1.size());      // 0
        l1.add("0");
        l1.add("1");
        l1.add(null);
        System.out.println(l1.size());      // 3
    }

    @Test
    public void testArrayList() {
        String[] sarray = {};
        String[] tarray = new String[]{""};
        System.out.println(Arrays.toString(sarray));
        System.out.println(sarray.length);      // 0
        System.out.println(Arrays.toString(tarray));
        System.out.println(tarray.length);      // 1
    }

    @Test
    public void testAddAll() {
        List<String> l1 = new ArrayList<>(3);
        List<String> l2 = new ArrayList<>(6);
        l1.add("1");l1.add("2");l1.add("3");
        l2.add("4");l2.add("5");l2.add("6");
        l1.addAll(l2);
    }

    @Test
    public void testModi() {
        List<String> l1 = new ArrayList<>();
        l1.add("1");l1.add("2");l1.add("3");
        l1.add("4");l1.add("5");l1.add("6");
//        for(int i = 0; i < l1.size(); i++) {
//            System.out.println(l1.get(i));
//            l1.remove(i);
//        }
        Iterator<String> it = l1.iterator();
        int i = 0;
        while(it.hasNext()) {
            l1.remove(i);
            i++;
            it.next();
        }
    }
}

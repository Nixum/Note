package other;

import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

public class TestOther {

    Integer a;

    @Test
    public void testArrayTransmit() {
        char[] t = new char[]{'c', 'm', 'x', 'n', 'b'};
        changeCharArray(t);
        System.out.println(t);

    }

    public void changeCharArray(char[] t) {
        t[0] = 'a';
    }

    @Test
    public void testPacketAndUnPacket() {
        Integer c = 127;
        Integer d = 127;
        System.out.println(c == d); // false

        Integer a = new Integer(129);
        int b = 129;
        System.out.println(a == b); // false
    }

    Integer ii = 0;

    @Test
    public void testString() {
        String a = new String("123");
        String b = "123" + new String("321");
        System.out.println(a.equals(b));

        TestOther qwe = new TestOther();
        System.out.println(qwe.a);
        System.out.println(Season.SUMMER.name);

        System.out.println(ii);

        int[] aa = new int[10];
        for(int i=0; i<aa.length; i++) {
            System.out.print(aa[i]);
        }
        System.out.println();
        byte x = 64;
        byte y = (byte)(x<<2);
        System.out.println("asd: " + y);

        Double dd = 24.3d;
        System.out.println(dd.equals(23.3 + 1));
    }

    @Test
    public void testSubsets() {
        int[] nums = new int[]{1,2,3,4,5};
        int target = 5;

        List<List<Integer>> resultList = new ArrayList<>();
        List<List<Integer>> allResultList = new ArrayList<>();
        List<Integer> first = new ArrayList<>();
        allResultList.add(first);

        for(int i = 0; i < nums.length; i++) {
            int length = allResultList.size();
            for(int j = 0; j < length; j++) {
                List<Integer> item = new ArrayList<>();
                item.addAll(allResultList.get(j));
                item.add(nums[i]);
                allResultList.add(item);

                int sum = 0;
                for (int k = 0; k < item.size(); k++) {
                    sum += item.get(k);
                }
                if (sum == target)
                    resultList.add(item);
            }
        }
        System.out.println(resultList);
    }

    @Test
    public void testMatrix() {

        char[][] a = new char[2][3];
        System.out.println(a[0][0]);
        for(int i = 0; i < a.length; i++) {
            for(int j = 0; j < a[i].length; j++) {
                System.out.print(a[i][j]);
            }
            System.out.println();
        }
        char asd = 'a';
        System.out.println(asd);

        int diff = 6;
        diff &= -diff;
        System.out.println(diff);
    }

    @Test
    public void testInteger() {
        Integer a = new Integer(1);
        add(a);
        System.out.println("result: " + a);
    }

    public void add(Integer a) {
        a += new Integer(1);
        System.out.println(a);
    }
}


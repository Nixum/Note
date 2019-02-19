package other;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

public class TestOther {

    @Test
    public void testArrayTransmit() {
        char[] t = new char[]{'c','m','x','n','b'};
        changeCharArray(t);
        System.out.println(t);

    }

    public void changeCharArray(char[] t) {
       t[0] = 'a';
    }

}

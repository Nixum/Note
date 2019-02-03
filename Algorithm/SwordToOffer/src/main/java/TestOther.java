import org.junit.Test;

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

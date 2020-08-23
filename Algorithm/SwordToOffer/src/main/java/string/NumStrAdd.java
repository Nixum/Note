package string;

import org.junit.Test;

public class NumStrAdd {

  @Test
  public void test() {
    System.out.println(add1("3234357", "96"));
  }

  /**
   * 两个正数字符串相加，不考虑字符串长度转整型溢出的情况，
   */
  public String add1(String a, String b) {
    StringBuilder tmp = new StringBuilder();
    int aLen = a.length() - 1;
    int bLen = b.length() - 1;
    int addOne = 0;
    while (aLen >= 0 || bLen >= 0) {
      int aNum = aLen >= 0 ? a.charAt(aLen) - '0' : 0;
      int bNum = bLen >= 0 ? b.charAt(bLen) - '0' : 0;
      addOne = aNum + bNum + addOne;
      tmp.append(addOne % 10);
      addOne /= 10;
      aLen--;
      bLen--;
    }
    return tmp.reverse().toString();
  }

}

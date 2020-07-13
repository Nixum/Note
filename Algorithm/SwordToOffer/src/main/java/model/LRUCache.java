package model;

import java.util.HashMap;
import java.util.Map;

// 非线程安全，注意每一次对节点有操作对需要同时操作 nodeMap和lruQueue
// LRU本质是利用 hashMap 和 双向链表 实现
public class LRUCache {

  private Map<String, Node> nodeMap;
  private DoubleLinkedList lruQueue;
  private int size = 0;

  public LRUCache() {
    this(3);
  }

  public LRUCache(int size) {
    this.size = size;
    this.nodeMap = new HashMap<>();
    this.lruQueue = new DoubleLinkedList();
  }


  public String get(String key) {
    Node n = nodeMap.get(key);
    if (n == null) {
      return null;
    }
    // 获取后直接放到到最前的位置
    put(n.key, n.value);
    return n.value;
  }

  public void put(String key, String value) {
    Node newNode = new Node(key, value);
    // 如果包含，则放到最前
    if (nodeMap.containsKey(key)) {
      lruQueue.remove(nodeMap.get(key));
      lruQueue.addFirst(newNode);
      // 记得更新map
      nodeMap.put(key, newNode);
    } else {
      // 如果满了，则移除最后一个
      if (size <= lruQueue.size()) {
        Node last = lruQueue.removeLast();
        nodeMap.remove(last.key);
      }
      nodeMap.put(key, newNode);
      lruQueue.addFirst(newNode);
    }
  }

  @Override
  public String toString() {
    Node head = lruQueue.head;
    StringBuilder s = new StringBuilder();
    while(head != null) {
      s = s.append(head.key);
      if (head.next != null) {
        s = s.append(", ");
      }
      head = head.next;
    }
    return s.toString();
  }

  private class Node {
    private String key;
    private String value;
    private Node pre;
    private Node next;

    public Node(String key, String value) {
      this.key = key;
      this.value = value;
    }
  }

  // 封装双向链表方法，构建时要注意前后节点指向和空指针问题
  private class DoubleLinkedList {
    private Node head;
    private Node tail;
    private int count = 0;

    public void addFirst(Node n) {
      if (head == null) {
        tail = n;
      }
      count ++;
      n.next = head;
      if (head != null) {
        head.pre = n;
      }
      head = n;
    }

    public Node removeLast() {
      if (count == 0) {
        return null;
      }
      Node result = tail;
      if (tail.pre != null) {
        tail.pre.next = null;
      } else {
        head = null;
        tail = null;
      }
      count --;
      return result;
    }

    public void remove(Node n) {
      if (count == 0) {
        return ;
      }
      count --;
      if (n.pre != null) {
        n.pre.next = n.next;
      } else {
        head = n.next;
      }
      if (n.next != null) {
        n.next.pre = n.pre;
      } else {
        tail = n.pre;
      }
    }

    public int size() {
      return count;
    }
  }

  public static void main(String[] args) {
    LRUCache cache = new LRUCache();
    cache.put("1", "11");
    cache.put("2", "22");
    cache.put("3", "33");
    System.out.println(cache.toString()); // 3, 2, 1
    System.out.println(cache.get("1"));   // 11
    System.out.println(cache.toString()); // 1, 3, 2
    System.out.println(cache.get("3"));   // 33
    System.out.println(cache.toString()); // 3, 1, 2
    cache.put("4", "44");
    System.out.println(cache.toString()); // 4, 3, 1
  }
}

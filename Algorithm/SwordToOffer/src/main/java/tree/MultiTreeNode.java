package tree;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.Stack;

public class MultiTreeNode {

  public Integer value;
  public List<MultiTreeNode> childes;

  public MultiTreeNode(int v) {
    this.value = v;
    this.childes = new ArrayList<>();
  }

  public static MultiTreeNode buildDemo() {
    MultiTreeNode root = new MultiTreeNode(0);
    for (int i = 1; i < 4; i++) {
      MultiTreeNode node = new MultiTreeNode(i);
      for (int j = 6; j < 10; j++) {
        MultiTreeNode threeLayerNode = new MultiTreeNode(i * j);
        node.childes.add(threeLayerNode);
      }
      root.childes.add(node);
    }
    return root;
  }

  public void bfs() {
    Queue<MultiTreeNode> queue = new LinkedList<>();
    queue.add(this);
    while (!queue.isEmpty()) {
      MultiTreeNode node = queue.remove();
      System.out.print(node.value + " ");
      for (int i = 0; i < node.childes.size(); i++) {
        queue.add(node.childes.get(i));
      }
    }
  }

  public void dfs() {
    Stack<MultiTreeNode> stack = new Stack<>();
    stack.push(this);
    while (!stack.isEmpty()) {
      MultiTreeNode node = stack.pop();
      System.out.print(node.value + " ");
      for (int i = 0; i < node.childes.size(); i++) {
        stack.push(node.childes.get(i));
      }
    }
  }

  public static void main(String[] args) {
    MultiTreeNode root = MultiTreeNode.buildDemo();
    root.bfs();
    System.out.println();
    root.dfs();
  }
}

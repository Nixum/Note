package graph;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * 求非连通图的连通分量的数量
 */
public class SumOfNoConnectedComponent {

    @Test
    public void test() {
        int[][] map = new int[][]{
                {0,1,0,1,0,0,0,0},
                {1,0,1,0,0,0,0,1},
                {0,1,0,0,0,0,0,0},
                {1,0,0,0,0,0,0,0},
                {0,0,0,0,0,1,0,0},
                {0,0,0,0,1,0,1,0},
                {0,0,0,0,0,1,0,0},
                {1,0,0,0,0,0,0,0}
        };
        System.out.println(countSumOfNoConnectedComponent(map));
    }

    // 深度优先搜索遍历求非连通图的连通分量的数量
    public int countSumOfNoConnectedComponent(int[][] graph) {
        if (graph == null)
            return 0;
        List<List<Integer>> resultList = new ArrayList<>();
        int sumOfNode = graph.length;
        int[] visited = new int[sumOfNode];
        int count = 0;
        for (int i = 0; i < sumOfNode; i++) {
            if (visited[i] == 0) {
                count ++;
                List<Integer> res = new ArrayList<>();
                dfs(graph, i, visited, res);
                resultList.add(res);
            }
        }
        System.out.println(resultList);
        return count;
    }

    public void dfs(int[][] graph, int index, int[] visited, List<Integer> res) {
        visited[index] = 1;
        res.add(index);
        for (int col = 0; col < graph[index].length; col++) {
            if (visited[col] == 0 && graph[index][col] == 1) {
                dfs(graph, col, visited, res);
            }
        }
    }
}

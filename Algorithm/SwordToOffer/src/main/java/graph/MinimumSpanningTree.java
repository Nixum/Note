package graph;

import org.junit.Test;

import java.util.*;

/**
 * 无向图带权值，求最小生成树
 * 克鲁斯卡尔算法
 */
public class MinimumSpanningTree {

    @Test
    public void test() {
        int[][] map = new int[][]{
                {-1,6,1,5,-1,-1},
                {6,-1,5,-1,3,-1},
                {1,5,-1,5,6,4},
                {5,-1,5,-1,-1,2},
                {-1,3,6,-1,-1,4},
                {-1,-1,4,2,4,-1}
        };
        findMinimumSpanningTree(map);
    }

    public void findMinimumSpanningTree(int[][] graph) {

        // 找出所有边，并存起来
        List<Edge> edgeList = new ArrayList<>();
        for (int i = 0; i < graph.length; i++) {
            for (int j = 0; j < graph[i].length; j++) {
                if (graph[i][j] != -1)
                    edgeList.add(new Edge(i, j, graph[i][j]));
            }
        }
        // 将所有边按权值从小到大排序
        Collections.sort(edgeList, new Comparator<Edge>() {
            @Override
            public int compare(Edge o1, Edge o2) {
                return o1.w < o2.w ? -1 : 1;
            }
        });
        // 用于记录连了哪些边，值相同的下标表示该点有连通
        int[] vset = new int[graph.length];
        for (int i = 0; i < graph.length; i++)
            vset[i] = i;
        // 取边
        int k = 1, j = 0;
        while (k < graph.length) {
            int u = edgeList.get(j).u;
            int v = edgeList.get(j).v;
            int s1 = vset[u];
            int s2 = vset[v];
            if (s1 != s2) {
                System.out.println("{" + u + ", " + v + ", " + edgeList.get(j).w + "}");
                k++; // 边数+1
                for (int i = 0; i < vset.length; i++) {
                    // 连通的边赋予相同的值
                    if (vset[i] == s2)
                        vset[i] = s1;
                }
            }
            j++;
        }
        System.out.println(Arrays.toString(vset));
    }

    class Edge {
        int u;
        int v;
        int w;

        public Edge(int u, int v, int w) {
            this.u = u;
            this.v = v;
            this.w = w;
        }

        @Override
        public String toString() {
            return "Edge{" +
                    "u=" + u +
                    ", v=" + v +
                    ", w=" + w +
                    '}';
        }
    }
}

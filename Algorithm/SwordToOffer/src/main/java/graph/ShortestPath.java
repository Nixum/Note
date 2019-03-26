package graph;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;

/**
 * 带权值有向图的各个结点到各个结点的最短路径
 * 弗洛伊德算法
 */
public class ShortestPath {

    public static final int MAX = 100;

    @Test
    public void test() {
        int[][] map = new int[][]{
                {0,5,MAX,7},
                {MAX,0,4,2},
                {3,3,0,2},
                {MAX,MAX,1,0}
        };
        floyd(map);
    }

    public void floyd(int[][] graph) {
        int n = graph.length;
        // path数组用于保存 点i到j 的最短路径中点j的前一个点的编号
        int[][] path = new int[n][n];
        // result表示点i到点j所的最小路径的权值和
        int[][] result = new int[n][n];
        for (int i = 0; i < n; i++) {
            for (int j = 0; j < n; j++) {
                result[i][j] = graph[i][j];
                if (i != j && graph[i][j] != -1)
                    path[i][j] = i;
                else
                    path[i][j] = -1;
            }
        }
        // 两个结点之间权值和如果比加入第三个结点的权值大，
        // 那么两个结点之间的最小权值即是加入第三个结点的权值之和
        // 表示 点i 到 点j 经过点k 的权值和 与 点i到点j的权值之和 比较，取小的
        for (int k = 0; k < n; k++) {
            for (int i = 0; i < n; i++) {
                for (int j = 0; j < n; j++) {
                    if (result[i][j] > result[i][k] + result[k][j]) {
                        result[i][j] = result[i][k] + result[k][j];
                        path[i][j] = path[k][j];
                    }
                }
            }
        }
        for (int i = 0; i < n; i++)
            System.out.println(Arrays.toString(result[i]));
        System.out.println();
        for (int i = 0; i < n; i++)
            System.out.println(Arrays.toString(path[i]));
    }
}

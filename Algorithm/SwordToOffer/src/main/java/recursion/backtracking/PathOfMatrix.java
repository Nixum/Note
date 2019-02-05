package recursion.backtracking;

import org.junit.Test;

/** 回溯法解决
 * 判断在一个矩阵中是否存在一条包含某字符串所有字符的路径。
 * 路径可以从矩阵中的任意一个格子开始，每一步可以在矩阵中向左，向右，向上，向下移动一个格子。
 * 如果一条路径经过了矩阵中的某一个格子，则该路径不能再进入该格子，即每个格子只能走一次
 * 如有以下矩阵
 * a    b   t   g
 * c    f   c   s
 * j    d   e   h
 * 路径bfce包含其中
 */
public class PathOfMatrix {

    @Test
    public void test() {
        char[][] matrix = new char[][]{{'a','b','t','g'}, {'c','f','c','s'}, {'j','d','e','h'}};
        System.out.println(hasPath(matrix, "acjdfcsg".toCharArray()));
    }

    /** 需要一个跟矩阵一样的二维数组来表示该值是否已经走过，防止重复；这里将递归过程看成一个栈
     * 遍历矩阵每个字符，判断是否符合path，符合时加入到栈中，以栈顶元素为中心，从右下左上的顺序开始找，
     * 判断是否符合path，符合时入栈，重复，当不符合时，换个方向继续找，当所有方向都不符合时，栈顶元素出栈，
     * 以此时的栈顶元素为中心继续按方向顺序找，如此反复，直到遍历到最后一个元素，或者找到path，返回
     */
    public boolean hasPath(char[][] matrix, char[] path) {
        if (matrix == null || path == null) {
            return false;
        }
        boolean flag = false;
        // 判断该位置是否被走过
        boolean visited[][] = new boolean[matrix.length][matrix[0].length];
        int pathIndex = 0;
        for (int i =  0; i < matrix.length; i++) {
            for(int j = 0; j < matrix[i].length; j++) {
                flag = changeDirection(matrix, path, i, j, pathIndex,visited);
                if (flag == true) {
                    return flag;
                }
            }
        }
        return false;
    }

    private boolean changeDirection(char[][] matrix, char[] path, int i, int j, int pathIndex, boolean[][] visited) {
        // 判断是否越界
        if (j >= matrix[0].length || i >= matrix.length || j < 0 || i < 0 || pathIndex >= path.length) {
            return false;
        }
        if (matrix[i][j] == path[pathIndex]) {
            visited[i][j] = true;
            pathIndex++;
            // 四个方向，从右，下，左，上，一直递归，直到无路可找，或者找到路径
            boolean flag = changeDirection(matrix, path, i, j + 1, pathIndex, visited)
            || changeDirection(matrix, path, i + 1, j, pathIndex, visited)
            || changeDirection(matrix, path, i, j - 1, pathIndex, visited)
            || changeDirection(matrix, path, i - 1, j, pathIndex, visited);
            // 如果全是false，说明该值的四个方向全错，回复到之前的状态
            if (flag == false && pathIndex != path.length) {
                visited[i][j] = false;
                pathIndex--;
            } else {
                return true;
            }
        }
        return false;
    }
}

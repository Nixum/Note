package array;

/**
 * 给你一个大小为 m x n 二维字符矩阵board ，表示扫雷游戏的盘面，其中：
 *
 * 'M'代表一个 未挖出的 地雷，
 * 'E'代表一个 未挖出的 空方块，
 * 'B'代表没有相邻（上，下，左，右，和所有4个对角线）地雷的 已挖出的 空白方块，
 * 数字（'1' 到 '8'）表示有多少地雷与这块 已挖出的 方块相邻，
 * 'X'则表示一个 已挖出的 地雷。
 * 给你一个整数数组 click ，其中 click = [clickr, clickc]
 * 表示在所有 未挖出的 方块（'M' 或者 'E'）中的下一个点击位置（clickr 是行下标，clickc 是列下标）。
 *
 * 根据以下规则，返回相应位置被点击后对应的盘面：
 * 如果一个地雷（'M'）被挖出，游戏就结束了- 把它改为'X' 。
 * 如果一个 没有相邻地雷 的空方块（'E'）被挖出，修改它为（'B'），并且所有和其相邻的 未挖出 方块都应该被递归地揭露。
 * 如果一个 至少与一个地雷相邻 的空方块（'E'）被挖出，修改它为数字（'1' 到 '8' ），表示相邻地雷的数量。
 * 如果在此次点击中，若无更多方块可被揭露，则返回盘面。
 *
 * 输入：click = [3,0]
 * ["E","E","E","E","E"]
 * ["E","E","M","E","E"]
 * ["E","E","E","E","E"]
 * ["E","E","E","E","E"]
 * 输出：
 * ["B","1","E","1","B"]
 * ["B","1","M","1","B"]
 * ["B","1","1","1","B"]
 * ["B","B","B","B","B"]
 *
 * 输入：click = [1,2]
 * ["B","1","E","1","B"]
 * ["B","1","M","1","B"]
 * ["B","1","1","1","B"]
 * ["B","B","B","B","B"]
 * 输出：
 * ["B","1","E","1","B"]
 * ["B","1","X","1","B"]
 * ["B","1","1","1","B"]
 * ["B","B","B","B","B"]
 */
public class Minesweeper {

    /**
     * 思路：dfs + 扫雷规则即可
     */
    // 定义 8 个方向
    int[] dx = {-1, 1, 0, 0, -1, 1, -1, 1};
    int[] dy = {0, 0, -1, 1, -1, 1, 1, -1};

    public char[][] updateBoard(char[][] board, int[] click) {
        int x = click[0], y = click[1];
        // 1. 若起点是雷，游戏结束，直接修改 board 并返回。
        if (board[x][y] == 'M') {
            board[x][y] = 'X';
        } else { // 2. 若起点是空地，则从起点开始向 8 邻域的空地进行深度优先搜索。
            dfs(board, x, y);
        }
        return board;
    }

    private void dfs(char[][] board, int i, int j) {
        // 递归终止条件：判断空地 (i, j) 周围是否有雷，若有，则将该位置修改为雷数，终止该路径的搜索。
        int cnt = 0;
        for (int k = 0; k < 8; k++) {
            int x = i + dx[k];
            int y = j + dy[k];
            if (x < 0 || x >= board.length || y < 0 || y >= board[0].length) {
                continue;
            }
            if (board[x][y] == 'M') {
                cnt++;
            }
        }
        if (cnt > 0) {
            board[i][j] =  (char)(cnt + '0');
            return;
        }

        // 若空地 (i, j) 周围没有雷，则将该位置修改为 ‘B’，向 8 邻域的空地继续搜索。
        board[i][j] = 'B';
        for (int k = 0; k < 8; k++) {
            int x = i + dx[k];
            int y = j + dy[k];
            if (x < 0 || x >= board.length || y < 0 || y >= board[0].length || board[x][y] != 'E') {
                continue;
            }
            dfs(board, x, y);
        }
    }

}

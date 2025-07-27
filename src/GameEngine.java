// File 1: GameEngine.java (游戏核心逻辑)
package src;

import java.util.Random;

import src.Enums.Difficulty;
import src.Enums.GameState;

// @qyx
public class GameEngine {

    private Difficulty difficulty;
    // 游戏配置
    private int rows;
    private int cols;
    private int mineCount;

    // 游戏状态
    // UI显示的数组
    private int[][] field;  // 游戏地图(1 - 8为数字，0为空格)
    private int[][] visit;  // visit记录已翻开的格子，0 为未翻开，1 为翻开
    int count; // 记录翻开的格子数量
    private int[][] state;  // state记录雷的位置，空地为 0，雷为 1 
    private int[][] flag;   // flag记录插旗位置, 0 为未插旗，1 为插旗
    private int flagsPlaced;         // 已放置的旗子数量
    private GameState gameState;     // 当前游戏状态
    private long startTime;          // 游戏开始时间

    public GameEngine() {
        difficulty = Difficulty.EASY;
        rows = 9;
        cols = 9;
        mineCount = 10;
    }

    public void setDifficulty(Difficulty dif) {
        this.difficulty = dif;
        // 根据难度设置参数
        switch (dif) {
            case EASY -> {
                rows = 9;
                cols = 9;
                mineCount = 10;
            }
            case MEDIUM -> {
                rows = 16;
                cols = 16;
                mineCount = 40;
            }
            case HARD -> {
                rows = 16;
                cols = 30;
                mineCount = 99;
            }
        }
    }
    // 初始化游戏

    public void initGame() {
        field = new int[rows][cols];
        visit = new int[rows][cols];
        state = new int[rows][cols];
        flag = new int[rows][cols];
        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < cols; j++) {
                field[i][j] = 0;
                visit[i][j] = 0;
                state[i][j] = 0;
                flag[i][j] = 0;
            }
        }
        flagsPlaced = 0;
        count = 0;
        gameState = GameState.PLAYING;

        // 当前时间
        startTime = System.currentTimeMillis();

        // 生成地雷和数字
        generateMines();
    }

    // 生成地雷位置
    private void generateMines() {
        // TODO: 随机生成地雷位置
        Random rand = new Random();
        for (int i = 0; i < mineCount; i++) {
            int row = rand.nextInt(rows);
            int col = rand.nextInt(cols);
            if (state[row][col] == 0) {
                state[row][col] = 1;
            } else {
                i--;
            }
        }

        // 生成数字
        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < cols; j++) {
                if (state[i][j] != 1) {
                    calculateNumbers(i, j);
                }
            }
        }
    }

    // 翻开格子
    public void revealCell(int row, int col) {
        // @wt
        // TODO: 实现翻开逻辑（包括递归翻开空白区域）
        if (visit[row][col] == 1 || flag[row][col] == 1) {
            return;
        }

        if (state[row][col] == 1) {
            gameState = GameState.LOST;
            return;
        }

        visit[row][col] = 1;
        count++;

        // 数字格子停止递归
        if (field[row][col] > 0) {
            return;
        }

        // 空白格子递归翻开
        for (int i = row - 1; i <= row + 1; i++) {
            for (int j = col - 1; j <= col + 1; j++) {
                if (i >= 0 && i < rows && j >= 0 && j < cols
                        && visit[i][j] == 0 && flag[i][j] == 0) {
                    revealCell(i, j);
                }
            }
        }
        checkWinCondition();
    }

    // 点击已经翻开的数字格
    public void clickNumber(int row, int col) {
        // @qyx
        // TODO: 实现点击数字逻辑（判断，符合的翻开周围格子）
        if (field[row][col] == 0) {
            return;
        }
        // 旗子数量
        int cnt = 0;
        // 正确拜访的旗子数量
        int corcnt = 0;
        for (int i = row - 1; i <= row + 1; i++) {
            for (int j = col - 1; j <= col + 1; j++) {
                if (i >= 0 && i < rows && j >= 0 && j < cols && flag[i][j] == 1) {
                    cnt++;
                    if (state[i][j] == 1) {
                        corcnt++;
                    }
                }
            }
        }
        if (cnt == field[row][col]) {
            if (cnt == corcnt) {
                // 正确翻开数字
                for (int i = row - 1; i <= row + 1; i++) {
                    for (int j = col - 1; j <= col + 1; j++) {
                        if (i >= 0 && i < rows && j >= 0 && j < cols
                                && state[i][j] == 0 && visit[i][j] == 0) {
                            revealCell(i, j); // 使用标准翻开逻辑
                        }
                    }
                }
            } else {
                gameState = GameState.LOST;
                return;
            }
        }
        checkWinCondition();
    }

    // 计算该格子周围雷的数量
    private void calculateNumbers(int row, int col) {
        // TODO: 计算周围雷的数量
        if (state[row][col] == 1) {
            return;
        }
        int cnt = 0;
        for (int i = row - 1; i <= row + 1; i++) {
            for (int j = col - 1; j <= col + 1; j++) {
                if (i >= 0 && i < rows && j >= 0 && j < cols && state[i][j] == 1) {
                    cnt++;
                }
            }
        }
        field[row][col] = cnt;
    }

    // 标记/取消标记格子
    public void toggleFlag(int row, int col) {
        // @wt
        // TODO: 切换旗子状态
        if (visit[row][col] == 1) {
            return;
        }
        if (flag[row][col] == 1) {
            flagsPlaced--;
            flag[row][col] = 0;
        } else if (flagsPlaced < mineCount) {
            flagsPlaced++;
            flag[row][col] = 1;
        }
    }

    // 检查游戏是否胜利
    private void checkWinCondition() {
        // TODO: 检查所有非雷格子是否都被翻开        
        if (rows * cols - mineCount == count) {
            gameState = GameState.WON;
        }
    }

    // 获取游戏经过时间（秒）
    public int getElapsedTime() {
        return (int) ((System.currentTimeMillis() - startTime) / 1000);
    }

    // 获取剩余旗子数量
    public int getRemainingFlags() {
        return mineCount - flagsPlaced;
    }

    // 获取游戏状态
    public GameState getGameState() {
        // TODO        
        return gameState;
    }

    // 获取游戏配置
    public int getRows() {
        // TODO
        return rows;
    }

    public int getCols() {
        // TODO
        return cols;
    }

    public int getMineCount() {
        // TODO
        return mineCount;
    }

    public Difficulty getDifficulty() {
        // TODO
        return difficulty;
    }

    // 按名称获取对应位置的格子状态
    public int getGridState(String name, int row, int col) {
        int[][] res = null;
        if (name.equals("field")) {
            res = field;
        } else if (name.equals("visit")) {
            res = visit;
        } else if (name.equals("state")) {
            res = state;
        } else if (name.equals("flag")) {
            res = flag;
        } else {
            throw new AssertionError();
        }

        return res[row][col];
    }
}

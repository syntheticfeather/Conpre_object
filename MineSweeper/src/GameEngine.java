// File 1: GameEngine.java (游戏核心逻辑)
package MineSweeper.src;

import MineSweeper.src.Enums.Difficulty;
import MineSweeper.src.Enums.GameState;

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
    private int[][] state;  // state记录雷的位置，空地为 0，雷为 1 
    private int[][] flag;   // flag记录插旗位置, 0 为未插旗，1 为插旗
    private int flagsPlaced;         // 已放置的旗子数量
    private GameState gameState;     // 当前游戏状态
    private long startTime;          // 游戏开始时间

    // 初始化游戏
    public void initGame(Difficulty difficulty) {
        // 根据难度设置参数
        switch (difficulty) {
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
        // 当前时间
        startTime = System.currentTimeMillis();

        // 生成地雷和数字
        generateMines();
    }

    // 生成地雷位置
    private void generateMines() {
        // TODO: 随机生成地雷位置
    }

    // 翻开格子
    public void revealCell(int row, int col) {
        // TODO: 实现翻开逻辑（包括递归翻开空白区域）
    }

    // 点击已经翻开的数字格
    public void clickNumber(int row, int col) {
        // TODO: 实现点击数字逻辑（判断，符合的翻开周围格子）
    }

    // 计算该格子周围雷的数量
    private void calculateNumbers(int row, int col) {
        // TODO: 计算周围雷的数量
    }

    // 标记/取消标记格子
    public void toggleFlag(int row, int col) {
        // TODO: 切换旗子状态
    }

    // 检查游戏是否胜利
    private void checkWinCondition() {
        // TODO: 检查所有非雷格子是否都被翻开
    }

    // 获取游戏经过时间（秒）
    public int getElapsedTime() {
        return (int) ((System.currentTimeMillis() - startTime) / 1000);
    }

    // 获取剩余旗子数量
    public int getRemainingFlags() {
        return mineCount - flagsPlaced;
    }

    // 获取格子状态（用于UI显示）
    public int[][] getfield() {
        // TODO        
    }

    // 获取游戏状态
    public GameState getGameState() {
        // TODO        
    }

    // 获取游戏配置
    public int getRows() {
        // TODO
    }

    public int getCols() {
        // TODO
    }

    public int getMineCount() {
        // TODO
    }

    public Difficulty getDifficulty() {
        // TODO
    }
}

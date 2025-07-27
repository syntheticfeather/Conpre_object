// File 1: GameEngine.java (游戏核心逻辑)
package MineSweeper.src;

import java.util.HashSet;
import java.util.Random;
import java.util.Set;

import MineSweeper.src.Enums.Difficulty;
import MineSweeper.src.Enums.GameState;
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
        // TODO
        
        // 当前时间
        startTime = System.currentTimeMillis();

        // 生成地雷和数字
        generateMines();
    }

    // 生成地雷位置
    private void generateMines() {
        //  随机生成地雷位置
        //  @wt
        int[][] temp=new int[mineCount][mineCount];
        Random random=new Random();
        Set<String>existingPairs=new HashSet<>();
        int i=0;
        while(i<mineCount){
            int row=random.nextInt(rows);
            int col=random.nextInt(rows);
            String pairKey=row+","+col;
            if(!existingPairs.contains(pairKey)){
                existingPairs.add(pairKey);
                temp[i][0]=row;
                temp[i][1]=col;
                i++;
            }
        }
        for(int j=0;j<mineCount;j++){
            state[temp[j][0]][temp[j][1]]=1;
        }

    }

    // 翻开格子
    public void revealCell(int row, int col) {
        // @wt
        // 实现翻开逻辑（包括递归翻开空白区域）
        if(visit[row][col]==1||flag[row][col]==1){
            return;
        }//已翻开或插旗不翻
        visit[row][col]=1;
        calculateNumbers(row,col);
        if(state[row][col]==1){
            gameState = GameState.LOST;
        }
        if(field[row][col]==0){
            revealCell(row-1, col);//上
            revealCell(row+1, col);//下
            revealCell(row, col-1);//左
            revealCell(row, col+1);//右
            revealCell(row-1, col-1);//左上
            revealCell(row-1, col+1);//右上
            revealCell(row+1, col-1);//左下
            revealCell(row+1, col+1);//右下
        }
        checkWinCondition();
    }

    // 点击已经翻开的数字格
    public void clickNumber(int row, int col) {
        // @qyx
        if (visit[row][col] == 1 && field[row][col] > 0) {
            int flaggedAround = 0;
            for (int r = Math.max(0, row - 1); r <= Math.min(rows - 1, row + 1); r++) {
                for (int c = Math.max(0, col - 1); c <= Math.min(cols - 1, col + 1); c++) {
                    if (flag[r][c] == 1) {
                        flaggedAround++;
                    }
                }
            }
            if (flaggedAround == field[row][col]) {
                revealCell(row-1, col); // 上
                revealCell(row+1, col); // 下
                revealCell(row, col-1); // 左
                revealCell(row, col+1); // 右
                revealCell(row-1, col-1); // 左上
                revealCell(row-1, col+1); // 右上
                revealCell(row+1, col-1); // 左下
                revealCell(row+1, col+1); // 右下
            }
        }
        //实现点击数字逻辑（判断，符合的翻开周围格子）
    }

    // 计算该格子周围雷的数量
    private void calculateNumbers(int row, int col) {
        //计算周围雷的数量
        //@wt
        int count=0;
        if(state[row-1][col]==1){
            count++;
        }//上
        if(state[row+1][col]==1){
            count++;
        }//下
        if(state[row][col-1]==1){
            count++;
        }//左
        if(state[row][col+1]==1){
            count++;
        }//右
        if(state[row-1][col-1]==1){
            count++;
        }//左上
        if(state[row-1][col+1]==1){
            count++;
        }//右上
        if(state[row+1][col-1]==1){
            count++;
        }//左下
        if(state[row+1][col+1]==1){
            count++;
        }//右下
        field[row][col]=count;
    }

    // 标记/取消标记格子
    public void toggleFlag(int row, int col) {
        // @wt
        // 切换旗子状态
        if(visit[row][col]==1){
            return;
        }
        if(flag[row][col]==0){
            if(flagsPlaced<=mineCount){
                flag[row][col]=1;
                flagsPlaced++;
            }
        }
        else{
            flag[row][col]=0;
            flagsPlaced--;
        }
    }

    // 检查游戏是否胜利
    private void checkWinCondition() {
        //检查所有非雷格子是否都被翻开
        boolean allNonMineRevealed = true;
        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < cols; j++) {
                if (state[i][j] == 0 && visit[i][j] == 0) {
                    allNonMineRevealed = false;
                    break;
                }
            }
        }
        if (allNonMineRevealed) {
            gameState = GameState.WON;
        }
        //@qyx
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
        return field;       
    }

    // 获取游戏状态
    public GameState getGameState() {
        return gameState;
    }

    // 获取游戏配置
    public int getRows() {
        return rows;
    }

    public int getCols() {
        return cols;
    }

    public int getMineCount() {
        return mineCount;
    }


    public Difficulty getDifficulty() {
        return difficulty;
    }

    // 按名称获取对应位置的格子状态
    public int getGridState(String name, int row, int col) {
        int[][] res = null;
        switch (name) {
            case "field" -> res = field;
            case "visit" -> res = visit;
            case "state" -> res = state;
            case "flag" -> res = flag;
            default -> throw new AssertionError();
        }
        return res[row][col];
    }
}
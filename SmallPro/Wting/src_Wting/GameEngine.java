// File 1: GameEngine.java (游戏核心逻辑)
package Wting.src_Wting;

import java.util.Random;
// import java.io.BufferedReader;
// import java.io.FileReader;
// import java.io.IOException;
//import java.util.logging.Level;

import Wting.src_Wting.Enums.GameState;

public class GameEngine {

    private int gameLevel;
    private int rows=10;
    private int cols=10;

    // 游戏状态
    // UI显示的数组
    private int[][] exist;  // 游戏地图(1存在，0不存在)
    private int[][] color;  // 星星颜色0-4, 5表示无颜色
    int Target; //目标分数
    int Score; //当前分数
    private GameState gameState;// 当前游戏状态

    // 初始化游戏*
    public void initGame() {
        exist = new int[rows][cols];
        color = new int[rows][cols];
        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < cols; j++) {
                exist[i][j] = 1;
                color[i][j] = 5;
            }
        }
        Target = 0;
        Score = 0;
        gameLevel=0;
        gameState = GameState.PLAYING;
        generateStars();
    }
    
    //计算当前关卡目标分
    private int getTarget(int level){
        if(level==1) return 1000;
        if(level==2) return 2500;
        if(level==3) return 4500;
        if(level==4) return 6500;
        if(level>=5){
            int target=6500+2500*(level-2);
            return target;
        } 
        throw new IllegalArgumentException("未知的关卡等级: " + level);
    }

    // 生成星星分布*
    public void generateStars(){
        int currentScore=Score;
        exist=new int[rows][cols];
        color=new int[rows][cols];
        for(int i=0;i<rows;i++){
            for(int j=0;j<cols;j++){
                exist[i][j]=1;
                color[i][j]=5;
            }
        }
        Random rand = new Random();
        do{
            for (int i = 0; i <rows; i++) {
                for(int j=0;j<cols;j++){
                    color[i][j]=rand.nextInt(5);
                }
            }
        }while(hasEliminatableStars()==false);
        
        gameLevel++;
        Target=getTarget(gameLevel);
        Score=currentScore;
        gameState=GameState.PLAYING;
    }
    
    //检查是否存在可消灭星星
    public boolean hasEliminatableStars(){
        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < cols; j++) {
                if (exist[i][j] == 1) {
                    // 添加边界检查
                    if (j > 0 && color[i][j] == color[i][j-1] && exist[i][j-1] == 1) return true;
                    if (i > 0 && color[i][j] == color[i-1][j] && exist[i-1][j] == 1) return true;
                    if (j < cols-1 && color[i][j] == color[i][j+1] && exist[i][j+1] == 1) return true;
                    if (i < rows-1 && color[i][j] == color[i+1][j] && exist[i+1][j] == 1) return true;
                 }
            }
        }
        return false;
    }
    
    //计算通关奖励
    public int calculateReward() {
        int count = 0;
        for (int i = 0; i<getRows(); i++) {
            for (int j = 0; j<getCols(); j++) {
                if (getStarState("exist", i, j) == 1) {
                    count++;
                }
            }
        }
        if(count<=10){
            int reward=50*(10-count);
            return reward;
        }
        else{ 
            return 0;
        }
    }

    public void addScore(int points) {
        Score += points;
    }

    //消灭星星*
    public void eliminateStars(int row, int col){
        if (exist[row][col] == 0){
            return;
        }
        int currentColor=color[row][col];
        int[][] directions={{-1,0},{1,0},{0,-1},{0,1}};
        boolean hasAdjacent=false;
        for (int[] dir:directions){
            int newRow=row+dir[0];
            int newCol=col+dir[1];
            if(newRow>=0&&newRow<rows&&newCol>=0&&newCol<cols){
                if(exist[newRow][newCol]==1&&color[newRow][newCol]==currentColor){
                    hasAdjacent=true;
                    break;
                }
            }
        }
        if(!hasAdjacent){
            return;
        }
        int count = eliminateConnectedStars(row, col, currentColor);
        Score += count * count * 5;
        checkPassCondition();
    }

    private int eliminateConnectedStars(int row, int col, int currentColor) {
        if (row < 0 || row >= rows || col < 0 || col >= cols || 
            exist[row][col] == 0 || color[row][col] != currentColor) {
            return 0;
        }
        exist[row][col] = 0;
        color[row][col] = 5;
        int count = 1;
        int[][] directions = {{-1,0}, {1,0}, {0,-1}, {0,1}};
        for (int[] dir : directions) {
            count += eliminateConnectedStars(row + dir[0], col + dir[1], currentColor);
        }
        return count;
    }

    //消灭星星后填补空缺
    public void fillStar(){
        // 垂直下落填充
        for (int j = 0; j < cols; j++) {
            int index=rows-1; 
            // 从下往上收集存在的星星
            for (int i = rows-1; i >= 0; i--) {
                if (exist[i][j] == 1) {
                    if(i!=index){
                        exist[index][j]=1;
                        color[index][j]=color[i][j];
                        exist[i][j]=0;
                        color[i][j]=5;
                    }
                    index--;
                }
            }
        }
            // 水平填充列
        int emptyCol=0;
        for (int j = 0; j <cols; j++) {
            boolean isEmpty=true;
            for(int i=0;i<rows;i++){
                if(exist[i][j]==1){
                    isEmpty=false;
                    break;
                }
            }
            if(!isEmpty){
                if(j!=emptyCol){
                    for(int i=0;i<rows;i++){
                        exist[i][emptyCol]=exist[i][j];
                        color[i][emptyCol]=color[i][j];
                        exist[i][j]=0;
                        color[i][j]=5;
                    }
                }
                emptyCol++;
            }
        }   
    }
    
    //道具1-炸弹：炸掉以指定星星为中心3*3所有星星*
    public void useBomb(int row, int col){
        exist[row][col]=0;
        exist[row+1][col]=0;
        exist[row-1][col]=0;
        exist[row][col+1]=0;
        exist[row][col-1]=0;
        exist[row+1][col+1]=0;
        exist[row+1][col-1]=0;
        exist[row-1][col+1]=0;
        exist[row-1][col-1]=0;
        Score=Score+405;
    }

    //道具2-颜料盘：指定星星颜色改为指定颜色*
    public void useColor(int row, int col, int input){
        color[row][col]=input;
    }

    //道具3-刷新：刷新星星布局*
    public void useRefresh(){
        generateStars();
    }

    //检查是否通关*
    public void checkPassCondition(){       
        // 只有在没有可消除星星时才检查通关条件
        if (!hasEliminatableStars()) {
            if (Score >= Target) {
                gameState = GameState.PASS;
            } else {
                gameState = GameState.LOST;
            }
        } else {
            // 还有可消除星星时保持PLAYING状态
            gameState = GameState.PLAYING;
        }
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

    public int getLevel() {
        return gameLevel;
    }
    
    public int getScore() {
        return Score;
    }
    
    public int getTarget() {
        return Target;
    }

    // 按名称获取对应位置的星星状态
    public int getStarState(String name, int row, int col) {
        if (name.equals("exist")) {
            return exist[row][col];
        } else if (name.equals("color")) {
            return color[row][col];
        }
        throw new AssertionError();
    }

    public void setStarState(String name, int row, int col, int value) {
        if(name.equals("exist")){
            exist[row][col] = value; 
        }else if (name.equals("color")) {
            color[row][col] = value;
        } else {
            throw new AssertionError();
        }
    }
}

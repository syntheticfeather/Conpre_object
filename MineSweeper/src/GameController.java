package MineSweeper.src;

// File 3: GameController.java (事件控制)
import MineSweeper.src.Enums.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.JButton;
import javax.swing.SwingUtilities;

public class GameController {

    private GameUI gameUI;
    private GameEngine gameEngine;

    public GameController(GameUI gameUI) {
        this.gameUI = gameUI;
    }

    // 设置游戏引擎
    public void setGameEngine(GameEngine gameEngine) {
        this.gameEngine = gameEngine;
    }

    // 为格子添加鼠标监听器
    public static void setupCellListeners(JButton cell, int row, int col,GameEngine gameEngine, GameUI gameUI){//添加了三个参数GameEngine gameEngine, GameUI gameUI
        cell.addMouseListener(new MouseAdapter() {                     //不加有报错：Cannot make a static reference to the non-static field gameEngine
            @Override
            public void mouseClicked(MouseEvent e) {
                //  @wt
                // 判断游戏状态
                // 左右键分别处理
                // 更新游戏状态
                if(gameEngine.getGameState()!=GameState.PLAYING){
                    return;
                }
                GameController controller=new GameController(gameUI);
                controller.setGameEngine(gameEngine);
                if(SwingUtilities.isLeftMouseButton(e)){
                    controller.handleLeftClick(row,col);
                }else if(SwingUtilities.isRightMouseButton(e)){
                    controller.handleRightClick(row,col);
                }
                gameUI.getGamePanel().updateGameBoard();
                controller.checkGameState();
            }
        });
    }

    // 处理左键点击（翻开格子）
    private void handleLeftClick(int row, int col) {
        // TODO: 实现翻开逻辑
        // @wt
        if(gameEngine.visit[row][col]==1||gameEngine.flag[row][col]==1){
            return;
        }
        gameEngine.revealCell(row, col);
        if(gameEngine.getGridState("state",row,col)==1){
            gameEngine.getGameState(GameState.LOST);//失败处理，直接弹出失败界面？
        }else{
            gameEngine.checkWinCondition();
        }
    }

    // 处理右键点击（标记旗子）
    private void handleRightClick(int row, int col) {
        // TODO: 实现旗子逻辑
        // @wt
        if(gameEngine.getGridState("visit", row, col)==1){
            return;
        }
        gameEngine.toggleFlag(row, col);
        gameUI.getGamePanel().updateFlagCount(gameEngine.getRemainingFlags());
        gameEngine.checkWinCondition();
    }

    // 检查并更新游戏状态
    private void checkGameState() {
        GameState state = gameEngine.getGameState();
        if (state != GameState.PLAYING) {
            int time;
            boolean isBestTime;
            // TODO: 检查是否为最佳时间
            gameUI.showEndScreen(time, isBestTime);
        }
    }
}

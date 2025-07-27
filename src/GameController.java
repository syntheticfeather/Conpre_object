package src;

// File 3: GameController.java (事件控制)
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.JButton;

import src.Enums.GameState;

public final class GameController {

    private final GameUI gameUI;
    private final GameEngine gameEngine;
    private final BestTimeManager bestTimeManager;

    public GameController() {
        this.gameEngine = new GameEngine();
        this.bestTimeManager = new BestTimeManager();
        this.gameUI = new GameUI(this, gameEngine);
    }

    // 为格子添加鼠标监听器
    public void setupCellListeners(JButton cell, int row, int col) {
        cell.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                //  @wt
                // 判断游戏状态                
                // 左右键分别处理
                if (gameEngine.getGameState() != GameState.PLAYING) {
                    return;
                }
                if (e.getButton() == MouseEvent.BUTTON1) {
                    // 左键点击
                    handleLeftClick(row, col);
                } else if (e.getButton() == MouseEvent.BUTTON3) {
                    // 右键点击
                    handleRightClick(row, col);
                }
                // 刷新游戏状态
                checkGameState();
                // 更新游戏状态           
                gameUI.getGamePanel().updateGameBoard();
                gameUI.getGamePanel().repaint();
            }
        });
    }

    // 处理左键点击（翻开格子）
    private void handleLeftClick(int row, int col) {
        // TODO: 实现翻开逻辑
        // @wt
        if (gameEngine.getGridState("visit", row, col) == 0) {
            gameEngine.revealCell(row, col);
        } else {
            gameEngine.clickNumber(row, col);
        }
    }

    // 处理右键点击（标记旗子）
    private void handleRightClick(int row, int col) {
        // TODO: 实现旗子逻辑
        // @wt
        gameEngine.toggleFlag(row, col);
    }

    // 检查并更新游戏状态
    private void checkGameState() {
        GameState state = gameEngine.getGameState();
        if (state != GameState.PLAYING) {
            int time = gameEngine.getElapsedTime();
            boolean isBestTime = bestTimeManager.checkAndSaveBestTime(gameEngine.getDifficulty(), time);
            // TODO: 检查是否为最佳时间            
            gameUI.showEndScreen(time, isBestTime, bestTimeManager.getBestTime(gameEngine.getDifficulty()));
        }
    }
}

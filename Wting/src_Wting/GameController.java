package Wting.src_Wting;

// File 3: GameController.java (事件控制)
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import javax.swing.JButton;
import java.lang.Thread;

import Wting.src_Wting.Enums.GameState;

public final class GameController {

    private final GameUI gameUI;
    public GameEngine gameEngine;
    private final BestScoreManager bestScoreManager;
    private boolean isEliminating = false; 

    public GameController() {
        this.gameEngine = new GameEngine(); 
        this.gameEngine.initGame();//初始化游戏
        this.bestScoreManager = new BestScoreManager();
        this.gameUI = new GameUI(this, gameEngine, bestScoreManager);
       
    }

    // 为格子添加鼠标监听器
    public void setupCellListeners(JButton cell, int row, int col) {
        cell.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                // 消除过程中或游戏未在进行中时点击无效
                if (isEliminating){
                    return;
                }
                if (gameEngine.getGameState() != GameState.LOST&&gameEngine.getGameState() != GameState.PLAYING&&gameEngine.getGameState() != GameState.PASS) {
                    return;
                }
                if (e.getButton() == MouseEvent.BUTTON1) {
                    //消灭星星
                    starCklick(row, col);
                }
                // 刷新游戏状态
                checkGameState();
                // 更新游戏状态           
                gameUI.getGamePanel().updateGameBoard();
                gameUI.getGamePanel().repaint();
            }
        });
    }

    // 处理左键点击（消灭星星）
    public void starCklick(int row, int col) {
        if(gameEngine.getGameState()!= GameState.PLAYING||gameEngine.getGameState()!=GameState.PASS){
            return;
        }
        if (gameEngine.getStarState("exist", row, col) == 1) {
            isEliminating = true;
            // 保存旧分数用于比较
            int oldScore = gameEngine.getScore();
            gameEngine.eliminateStars(row, col);
            gameEngine.fillStar();
            // 获取新分数
            int newScore = gameEngine.getScore();
            // 更新UI分数显示
            if (newScore != oldScore) {
                gameUI.getGamePanel().updateScore();
            }
            isEliminating = false;
        }
        // 确保更新游戏面板
        checkGameState();
        gameUI.getGamePanel().updateGameBoard();
        gameUI.getGamePanel().repaint();
    }

    // 检查并更新游戏状态
    private void checkGameState() {
        GameState state = gameEngine.getGameState();
        // 通关判定
        if (state == GameState.PASS) {
        // 通关时显示提示
            gameUI.getGamePanel().showPassMessage();
        }
        if (!gameEngine.hasEliminatableStars()) {
            int reward = gameEngine.calculateReward();
            gameEngine.addScore(reward);// 无可消除且通关后进入下一关
            gameEngine.checkPassCondition();
            eliminateRemaingStars();
            if(state == GameState.PASS){
                new Thread(() -> {
                    try {
                        Thread.sleep(2000);
                        gameEngine.generateStars(); // 生成下一关星星
                        gameUI.getGamePanel().updateGameBoard();
                        gameUI.getGamePanel().repaint();
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                }).start();
            }else {
                state=GameState.LOST;
                bestScoreManager.checkAndSaveBestScore(gameEngine.getScore(), gameEngine.getGameState());
                boolean isBest = gameEngine.getScore() == bestScoreManager.getBestScore();
                gameUI.showEndScreen(gameEngine.getScore(), isBest, bestScoreManager.getBestScore());
            }
        }
    }
    
    //道具使用 
    public void useBomb(int row, int col) {
        if (gameEngine.getGameState() == GameState.PLAYING && !isEliminating) {
            gameEngine.useBomb(row, col);
            gameEngine.fillStar();
            gameUI.getGamePanel().updateGameBoard();
            checkGameState();
        }
    }

    public void useColor(int row, int col, int color) {
        if (gameEngine.getGameState() == GameState.PLAYING && !isEliminating) {
            gameEngine.useColor(row, col, color);
            gameUI.getGamePanel().updateGameBoard();
        }
    }

    public void useRefresh() {
        if (gameEngine.getGameState() == GameState.PLAYING && !isEliminating) {
            gameEngine.useRefresh();
            gameUI.getGamePanel().updateGameBoard();
        }
    }
    
    public void eliminateRemaingStars(){
        for(int i=0;i<gameEngine.getRows();i++){
            for(int j=0;j<gameEngine.getCols();j++){
                if(gameEngine.getStarState("exist", i, j)==1){
                    gameEngine.setStarState("exist", i, j,0);
                    gameEngine.setStarState("color", i, j,5);
                }
            }   
        }
    }
    public GameUI getGameUI() {
        return gameUI;
    }

    public GameEngine getGameEngine() {
        return gameEngine;
    }
    
    public BestScoreManager getBestScoreManager() {
        return bestScoreManager;
    }
}

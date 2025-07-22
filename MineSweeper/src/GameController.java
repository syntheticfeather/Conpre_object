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
    public static void setupCellListeners(JButton cell, int row, int col) {
        cell.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {

                //  @wt
                // 判断游戏状态
                // 左右键分别处理
                // 更新游戏状态
            }
        });
    }

    // 处理左键点击（翻开格子）
    private void handleLeftClick(int row, int col) {
        // TODO: 实现翻开逻辑
        // @wt
        gameEngine.revealCell(row, col);
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
            int time;
            boolean isBestTime;
            // TODO: 检查是否为最佳时间
            gameUI.showEndScreen(time, isBestTime);
        }
    }
}

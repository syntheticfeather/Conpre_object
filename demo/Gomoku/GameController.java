package Gomoku;

import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.JButton;

import Gomoku.Enums.GameState;

// 事件处理：连接视图与模型，转发用户操作
public final class GameController {
    private final GameEngine engine; 
    private final GameUI ui; 

    public GameController() {
        this.engine = new GameEngine();
        this.ui = new GameUI(this, engine);
        setupEventListeners();
    }

    // 绑定棋盘按钮的点击事件
    public void setupCellListener(JButton cell, int row, int col) {
        cell.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                // 仅处理左键点击，且游戏处于进行中
                if (e.getButton() == MouseEvent.BUTTON1 && engine.getGameState() == GameState.PLAYING) {
                    boolean placed = engine.placePiece(row, col);
                    if (placed) {
                        ui.updateBoard();  // 落子成功后刷新界面
                    }
                }
            }
        });
    }

    //绑定界面按钮事件监听器
    private void setupEventListeners() {
        //重新开始
        ui.setRestartListener(e -> {
            engine.initGame();
            ui.updateBoard();
        });
        //悔棋
        ui.setUndoListener(e -> {
            if (engine.undoMove()) {
                ui.updateBoard();
            }
        });
    }
}
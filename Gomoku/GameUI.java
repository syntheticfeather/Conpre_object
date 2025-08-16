package Gomoku;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;

import Gomoku.Enums.GameState;
import Gomoku.Enums.PieceType;

// 界面展示：棋盘绘制、状态显示、按钮交互
public class GameUI {
    private final JFrame mainFrame;
    private final JPanel gamePanel;
    private final JPanel infoPanel;  // 显示当前玩家、游戏状态
    private final JButton[][] boardButtons;  // 棋盘按钮（用于点击落子）
    private final JLabel statusLabel;  // 状态提示（如"黑棋回合"）
    private final JButton restartButton;  // 重新开始按钮
    private final int CELL_SIZE = 40;  // 每个格子大小
    private final JButton undoButton;
    private final GameEngine engine;

    public GameUI(GameController controller, GameEngine engine) {
        this.engine = engine;
        int boardSize = engine.getBoardSize();
        mainFrame = new JFrame("五子棋");
        mainFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        // 初始化棋盘按钮
        boardButtons = new JButton[boardSize][boardSize];
        gamePanel = new JPanel(new GridLayout(boardSize, boardSize));
        initBoardButtons(controller);

        // 信息面板（状态+按钮）
        infoPanel = new JPanel();
        statusLabel = new JLabel("当前回合：黑棋", SwingConstants.CENTER);
        statusLabel.setFont(new Font("宋体", Font.BOLD, 16));
        restartButton = new JButton("重新开始");
        undoButton = new JButton("悔棋");
        infoPanel.add(statusLabel);
        infoPanel.add(restartButton);
        infoPanel.add(undoButton); 

        // 组装界面
        mainFrame.add(infoPanel, BorderLayout.NORTH);
        mainFrame.add(gamePanel, BorderLayout.CENTER);
        mainFrame.setSize(boardSize * CELL_SIZE + 50, boardSize * CELL_SIZE + 100);
        mainFrame.setVisible(true);
    }
    
    public void setUndoListener(ActionListener listener) {
        undoButton.addActionListener(listener);
    }

    // 初始化棋盘按钮（绑定点击事件）
    private void initBoardButtons(GameController controller) {
        int boardSize = engine.getBoardSize();
        for (int i = 0; i < boardSize; i++) {
            for (int j = 0; j < boardSize; j++) {
                JButton btn = new JButton();
                btn.setBackground(Color.LIGHT_GRAY);
                btn.setPreferredSize(new Dimension(CELL_SIZE, CELL_SIZE));
                // 绑定控制器的点击事件
                controller.setupCellListener(btn, i, j);
                boardButtons[i][j] = btn;
                gamePanel.add(btn);
            }
        }
    }

    // 更新棋盘界面
    public void updateBoard() {
        int boardSize = engine.getBoardSize();
        for (int i = 0; i < boardSize; i++) {
            for (int j = 0; j < boardSize; j++) {
                PieceType piece = engine.getPiece(i, j);
                if (piece == PieceType.BLACK) {
                    boardButtons[i][j].setBackground(Color.BLACK);
                } else if (piece == PieceType.WHITE) {
                    boardButtons[i][j].setBackground(Color.WHITE);
                }else{
                    boardButtons[i][j].setBackground(Color.LIGHT_GRAY);
                }
            }
        }
        // 更新状态提示
        updateStatusLabel();
    }

    // 更新状态标签（显示当前玩家或胜负结果）
    private void updateStatusLabel() {
        GameState state = engine.getGameState();
        switch (state) {
            case PLAYING:
                String player = engine.getCurrentPlayer() == PieceType.BLACK ? "黑棋" : "白棋";
                statusLabel.setText("当前回合：" + player);
                break;
            case BLACK_WIN:
                statusLabel.setText("游戏结束：黑棋获胜！");
                break;
            case WHITE_WIN:
                statusLabel.setText("游戏结束：白棋获胜！");
                break;
            case DRAW:
                statusLabel.setText("游戏结束：平局！");
                break;
        }
    }

    // 绑定重新开始按钮事件
    public void setRestartListener(ActionListener listener) {
        restartButton.addActionListener(listener);
    }
}
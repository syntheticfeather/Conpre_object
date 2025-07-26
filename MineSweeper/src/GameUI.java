package MineSweeper.src;

// File 2: GameUI.java (界面系统)
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.event.ActionListener;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.Timer;

import MineSweeper.src.Enums.Difficulty;
import MineSweeper.src.Enums.GameState;

// @zff qyx

public class GameUI {

    private JFrame mainFrame;
    private CardLayout cardLayout;
    private JPanel cardPanel;

    // 三个主要界面
    private StartPanel startPanel;
    private GamePanel gamePanel;
    private EndPanel endPanel;

    // 游戏引擎引用
    private GameEngine gameEngine;

    public GameUI() {
        // 初始化主窗口
        mainFrame = new JFrame("扫雷游戏");
        mainFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        mainFrame.setSize(1000, 500);

        // 创建卡片布局
        cardLayout = new CardLayout();
        cardPanel = new JPanel(cardLayout);

        // 初始化各个界面
        startPanel = new StartPanel();
        gamePanel = new GamePanel();
        endPanel = new EndPanel();

        // 添加界面到卡片面板
        cardPanel.add(startPanel, "START");
        cardPanel.add(gamePanel, "GAME");
        cardPanel.add(endPanel, "END");

        mainFrame.add(cardPanel);
        mainFrame.setVisible(true);

        // 绑定事件
        setupEventListeners();
    }

    // 设置事件监听器
    private void setupEventListeners() {
        // 设置按钮按下的事件
        startPanel.setStartButtonListener(e -> {
            Difficulty difficulty = startPanel.getSelectedDifficulty();
            startGame(difficulty);
            cardLayout.show(cardPanel, "GAME");
        });

        // 重新游戏
        gamePanel.setNewGameListener(e -> {
            startGame(gameEngine.getDifficulty());
            cardLayout.show(cardPanel, "GAME");
        });

        // 回到主菜单
        gamePanel.setBackToStartListener(e -> {
            cardLayout.show(cardPanel, "START");
        });
    }

    // 开始新游戏
    private void startGame(Difficulty difficulty) {
        gameEngine.initGame(difficulty);
        gamePanel.initGameBoard(gameEngine);
    }

    // 显示结束界面
    public void showEndScreen(int time, boolean isBestTime) {
        endPanel.setTime(time);
        endPanel.setBestTime(isBestTime);
        cardLayout.show(cardPanel, "END");
    }

    // 内部类：开始界面
    class StartPanel extends JPanel {

        // 一个可以选择难度的下拉框
        private JComboBox<Difficulty> difficultyCombo;
        // 一个可以点击得按钮
        private JButton startButton;

        private JPanel panel;
        private JLabel row, col, mineCount;

        public StartPanel() {
            // if (gameEngine == null) {
            //     gameEngine = new GameEngine();
            // }
            setLayout(null);
            // TODO: 构建开始界面UI
            JPanel panel = new JPanel(null);
            panel.setBounds(300, 100, 400, 250);
            panel.setBackground((new Color(59, 78, 127)));
            this.add(panel);

            difficultyCombo = new JComboBox<>(Difficulty.values());
            startButton = new JButton("开始游戏");
            // row = new JLabel("行数:" + gameEngine.getRows());
            // col = new JLabel("列数:" + gameEngine.getCols());
            // mineCount = new JLabel("雷数:" + gameEngine.getMineCount());
            // 上下布局
            difficultyCombo.setBounds(150, 50, 100, 30);
            startButton.setBounds(150, 200, 100, 30);
            // row.setBounds(120 ,100, 60, 30);
            // row.setBounds(220 ,100, 60, 30);
            // mineCount.setBounds(160, 150, 80, 30);

            panel.add(difficultyCombo);
            panel.add(startButton);
            // panel.add(row);
            // panel.add(col);
            // panel.add(mineCount);

            // 为 startbutton 设置图片
            ImageIcon icon = new ImageIcon("");
            startButton.setIcon(icon);

        }

        public void setStartButtonListener(ActionListener listener) {
            startButton.addActionListener(listener);
        }

        public Difficulty getSelectedDifficulty() {
            return (Difficulty) difficultyCombo.getSelectedItem();
        }
    }

    public GamePanel getGamePanel() {
        return gamePanel;
    }

    // 内部类：游戏界面
    class GamePanel extends JPanel {
        // @qyx zff
        private JButton newGameButton;
        private JButton backButton;
        private JLabel flagCountLabel;
        private JLabel timerLabel;
        private JPanel gridPanel;
        private Timer gameTimer;
        private GameEngine gameEngine;

        private ImageIcon unopenedIcon;
        private ImageIcon flagIcon;
        private ImageIcon mineIcon;
        private ImageIcon[] numberIcons;

        public GamePanel() {
            // TODO: 构建游戏界面UI
            newGameButton = new JButton("新游戏");
            backButton = new JButton("返回");
            flagCountLabel = new JLabel("旗子: 0");
            timerLabel = new JLabel("时间: 0秒");
            gridPanel = new JPanel();

            String basePath = "MineSweeper\\resources\\img\\";

            numberIcons = new ImageIcon[9];
            numberIcons[0] = new ImageIcon(basePath + "0.png"); 
            numberIcons[1] = new ImageIcon(basePath + "1.png");
            numberIcons[2] = new ImageIcon(basePath + "2.png");
            numberIcons[3] = new ImageIcon(basePath + "3.png");
            numberIcons[4] = new ImageIcon(basePath + "4.png");
            numberIcons[5] = new ImageIcon(basePath + "5.png");
            numberIcons[6] = new ImageIcon(basePath + "6.png");
            numberIcons[7] = new ImageIcon(basePath + "7.png");
            numberIcons[8] = new ImageIcon(basePath + "8.png");
            unopenedIcon = new ImageIcon(basePath + "-1.png");
            flagIcon = new ImageIcon(basePath + "f.png");
            mineIcon = new ImageIcon(basePath + "m.png");

            gameTimer = new Timer(1000, e -> {
                if (gameEngine != null && gameEngine.getGameState() == GameState.PLAYING) {
                    updateTimer(gameEngine.getElapsedTime());
                }
            });
        }

        public void initGameBoard(GameEngine gameEngine) {
            // TODO: 根据游戏引擎初始化游戏网格
            this.gameEngine = gameEngine;
            gridPanel.removeAll();
            int rows=gameEngine.getRows();
            int cols=gameEngine.getCols();
            for (int i = 0; i < rows; i++) {
                for (int j = 0; j < cols; j++) {
                    JButton cell = new JButton();
                    GameController.setupCellListeners(cell, i, j, gameEngine, GameUI.this);
                    gridPanel.add(cell);
                }
            }
            updateFlagCount(gameEngine.getRemainingFlags());
            updateTimer(0);
            gameTimer.start();
            revalidate();
        }

        public void updateGameBoard() {
            // TODO: 更新游戏网格显示
            if (gameEngine == null) return;
            Component[] cells = gridPanel.getComponents();
            int rows = gameEngine.getRows();
            int cols = gameEngine.getCols();
    
            for (int i = 0; i < rows; i++) {
                for (int j = 0; j < cols; j++) {
                    JButton cell = (JButton) cells[i * cols + j];
                    int visit = gameEngine.getGridState("visit", i, j);
                    int flag = gameEngine.getGridState("flag", i, j);
                    int field = gameEngine.getGridState("field", i, j);
                    int state = gameEngine.getGridState("state", i, j);
                    if (flag == 1) {
                        cell.setIcon(flagIcon); 
                    } else if (visit == 1) {
                        if (state == 1) {
                            cell.setIcon(mineIcon); 
                        } else {
                            cell.setIcon(numberIcons[field]);
                        }
                    } else {
                        cell.setIcon(unopenedIcon); 
                    }
                    cell.setText("");
                    }
                }
        }

        public void setNewGameListener(ActionListener listener) {
            newGameButton.addActionListener(listener);
        }

        public void setBackToStartListener(ActionListener listener) {
            backButton.addActionListener(listener);
        }

        public void updateFlagCount(int count) {
            flagCountLabel.setText("旗子: " + count);
        }

        public void updateTimer(int seconds) {
            timerLabel.setText("时间: " + seconds + "秒");
        }
    }

    // 内部类：结束界面
    class EndPanel extends JPanel {

        private JLabel resultLabel;
        private JLabel timeLabel;
        private JLabel bestTimeLabel;
        private JButton restartButton;
        private JButton backButton;

        public EndPanel() {
            // @qyx
            // TODO: 构建结束界面UI
            setLayout(null);
            setBackground(new Color(59, 78, 127));

            JPanel panel = new JPanel();
            panel.setBounds(300, 100, 400, 250);
            panel.setBackground(new Color(59, 78, 127));
            add(panel);

            resultLabel = new JLabel("游戏结束!");
            timeLabel = new JLabel("你的时间: 0秒");
            panel.add(resultLabel);
            panel.add(timeLabel);
            panel.add(bestTimeLabel); 
            panel.add(restartButton);
            panel.add(backButton);

        }

        public void setTime(int seconds) {
            timeLabel.setText("你的时间: " + seconds + "秒");
        }

        public void setBestTime(boolean isBest) {
            bestTimeLabel.setText(isBest ? "新纪录！" : "");
            bestTimeLabel.setVisible(isBest);
        }

        public void setRestartListener(ActionListener listener) {
            restartButton.addActionListener(listener);
        }

        public void setBackListener(ActionListener listener) {
            backButton.addActionListener(listener);
        }
    }
}

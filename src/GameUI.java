package src;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionListener;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

import src.Enums.Difficulty;
import src.Enums.GameState;

// @zff qyx
public class GameUI {

    private final JFrame mainFrame;
    private final CardLayout cardLayout;
    private final JPanel cardPanel;

    // 三个主要界面
    private final StartPanel startPanel;
    private final GamePanel gamePanel;
    private final EndPanel endPanel;

    int framewidth;

    // 游戏引擎引用
    private final GameEngine gameEngine;

    public GameUI(GameController gc, GameEngine gE) {
        // 初始化主窗口
        gameEngine = gE;
        mainFrame = new JFrame("扫雷游戏");
        mainFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        mainFrame.setSize(1000, 500);
        framewidth = mainFrame.getWidth();

        // 创建卡片布局
        cardLayout = new CardLayout();
        cardPanel = new JPanel(cardLayout);

        // 初始化各个界面
        startPanel = new StartPanel();
        gamePanel = new GamePanel(gc);
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
        startPanel.setStartButtonListener(_ -> {
            startGame();
            gamePanel.repaint();
            cardLayout.show(cardPanel, "GAME");
        });

        // 重新游戏
        gamePanel.setNewGameListener(_ -> {
            startGame();
            cardLayout.show(cardPanel, "GAME");
        });

        // 回到主菜单
        gamePanel.setBackToStartListener(_ -> {
            cardLayout.show(cardPanel, "START");
        });

        endPanel.setRestartListener(_ -> {
            startGame();
            gamePanel.repaint();
            cardLayout.show(cardPanel, "GAME");
        });

        endPanel.setBackListener(_ -> {
            cardLayout.show(cardPanel, "START");
        });
    }

    // 开始新游戏
    private void startGame() {
        gameEngine.initGame();
        gamePanel.initGameBoard(gameEngine);
        gamePanel.repaint();
    }

    // 显示结束界面
    public void showEndScreen(int time, boolean isBestTime, int bestTime) {
        endPanel.setTime(time);
        endPanel.setBestTime(isBestTime, bestTime);
        endPanel.isWon();
        endPanel.repaint();
        cardLayout.show(cardPanel, "END");
    }

    // 内部类：开始界面
    class StartPanel extends JPanel {

        // 一个可以选择难度的下拉框
        private final JComboBox<Difficulty> difficultyCombo;
        // 一个可以点击得按钮
        private final JButton startButton;

        private final JPanel panel;
        private final JLabel row, col, mineCount;

        public StartPanel() {
            setLayout(null);
            // TODO: 构建开始界面UI
            panel = new JPanel(null);
            difficultyCombo = new JComboBox<>(Difficulty.values());
            startButton = new JButton("开始游戏");
            row = new JLabel("行数:" + gameEngine.getRows());
            col = new JLabel("列数:" + gameEngine.getCols());
            mineCount = new JLabel("雷数:" + gameEngine.getMineCount());
            difficultyCombo.addItemListener(_ -> {
                // 难度变化时，参数一起变化
                gameEngine.setDifficulty(getSelectedDifficulty());
                row.setText("行数:" + gameEngine.getRows());
                col.setText("列数:" + gameEngine.getCols());
                mineCount.setText("雷数:" + gameEngine.getMineCount());
            });
            // 布局
            // @qyx
            // TODO
            this.setBackground((new Color(255, 255, 255)));
            panel.setBounds(300, 100, 400, 250);
            panel.setBackground((new Color(59, 78, 127)));
            difficultyCombo.setBounds(150, 50, 100, 30);
            startButton.setBounds(150, 200, 100, 30);
            row.setBounds(120, 100, 60, 30);
            col.setBounds(220, 100, 60, 30);
            mineCount.setBounds(160, 150, 80, 30);
            // 为 startbutton 设置图片
            startButton.setIcon(new ImageIcon("resources/img/start.jpg"));
            // 设置组件
            this.add(panel);
            panel.add(difficultyCombo);
            panel.add(startButton);
            panel.add(row);
            panel.add(col);
            panel.add(mineCount);

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

        final int CELL_SIZE = 72;
        // @qyx
        private final JScrollPane scrollPane;
        private final GameController gameController;
        private JButton newGameButton;
        private JButton backButton;
        private JLabel flagCountLabel;
        private JLabel timerLabel;
        private JPanel topMenuPanel;
        private JPanel gridPanel;
        private JButton[][] buttons;

        public GamePanel(GameController gC) {
            gameController = gC;
            setLayout(new BorderLayout()); // 改为 BorderLayout 布局
            // TODO: 构建游戏界面UI
            topMenuPanel = new JPanel(null);
            backButton = new JButton(new ImageIcon("resources/img/home.jpg"));
            newGameButton = new JButton(new ImageIcon("resources/img/new.jpg"));
            // 白字并且大小大一些
            flagCountLabel = new JLabel("旗子: " + gameEngine.getRemainingFlags(), JLabel.TRAILING);
            flagCountLabel.setFont(new Font("Microsoft YaHei", Font.BOLD, 20));
            flagCountLabel.setForeground(Color.WHITE);

            timerLabel = new JLabel("时间: " + 0 + "秒", JLabel.TRAILING);
            timerLabel.setFont(new Font("Microsoft YaHei", Font.BOLD, 20));
            timerLabel.setForeground(Color.WHITE);
            gridPanel = new JPanel();
            JPanel centerPanel = new JPanel(new GridBagLayout());
            GridBagConstraints gbc = new GridBagConstraints();
            gbc.gridx = 0;
            gbc.gridy = 0;
            gbc.anchor = GridBagConstraints.CENTER;
            centerPanel.add(gridPanel, gbc);
            // 创建滚动面板并将网格面板放入其中
            scrollPane = new JScrollPane(centerPanel);
            scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
            scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
            // TODO 
            // @qyx
            topMenuPanel.setPreferredSize(new Dimension(1000, 60));
            topMenuPanel.setBackground(new Color(15, 17, 26));
            backButton.setBounds(10, 10, 50, 50);
            newGameButton.setBounds(100, 10, 100, 50);
            flagCountLabel.setBounds(200, 10, 100, 30);
            timerLabel.setBounds(300, 10, 100, 30);

            topMenuPanel.add(backButton);
            topMenuPanel.add(newGameButton);
            topMenuPanel.add(flagCountLabel);
            topMenuPanel.add(timerLabel);
            this.add(topMenuPanel, BorderLayout.NORTH);
            this.add(scrollPane, BorderLayout.CENTER); // 滚动面板放在中心
        }

        public void initGameBoard(GameEngine gameEngine) {
            // TODO: 根据游戏引擎初始化游戏网格
            // TODO: 构建游戏界面UI
            // 初始化
            gridPanel.removeAll();
            buttons = null;
            int rows = gameEngine.getRows();
            int cols = gameEngine.getCols();
            buttons = new JButton[rows][cols];
            // 设置网格面板大小（根据单元格数量和大小）
            gridPanel.setPreferredSize(new Dimension(CELL_SIZE * cols, CELL_SIZE * rows));
            gridPanel.setLayout(null);
            gridPanel.setBackground(new Color(64, 76, 118));
            // 设置布局样式
            for (int i = 0; i < rows; i++) {
                for (int j = 0; j < cols; j++) {
                    // 设置图片
                    buttons[i][j] = new JButton(new ImageIcon("resources/img/-1.jpg"));
                    // 设置按钮位置
                    buttons[i][j].setBounds(j * CELL_SIZE, i * CELL_SIZE, CELL_SIZE, CELL_SIZE);
                    // 设置按钮点击事件
                    gameController.setupCellListeners(buttons[i][j], i, j);
                }
            }
            // 设置组件            
            for (JButton[] button : buttons) {
                for (JButton button1 : button) {
                    gridPanel.add(button1);
                }
            }
            // 重新验证和重绘
            gridPanel.revalidate();
            gridPanel.repaint();
            scrollPane.revalidate(); // 更新滚动面板
        }

        public void updateGameBoard() {
            // TODO: 更新游戏网格显示
            for (int i = 0; i < buttons.length; i++) {
                for (int j = 0; j < buttons[i].length; j++) {
                    // TODO: 更新按钮显示
                    if (gameEngine.getGridState("visit", i, j) == 0) {
                        // 没翻开
                        if (gameEngine.getGridState("flag", i, j) == 1) {
                            // 旗子
                            buttons[i][j].setIcon(new ImageIcon("resources/img/f.jpg"));
                        } else {
                            // 空地
                            buttons[i][j].setIcon(new ImageIcon("resources/img/-1.jpg"));
                        }
                    } else {
                        // 已经翻开了
                        buttons[i][j].setIcon(new ImageIcon("resources/img/" + gameEngine.getGridState("field", i, j) + ".jpg"));
                    }
                }
            }
            updateFlagCount(gameEngine.getRemainingFlags());
            // 更新计时器
            updateTimer(gameEngine.getElapsedTime() / 1000);
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

        public JButton[][] getButtons() {
            return buttons;
        }

    }

// 内部类：结束界面
    class EndPanel extends JPanel {

        private final JLabel resultLabel;
        private JLabel timeLabel;
        private JLabel bestTimeLabel;
        private final JButton restartButton;
        private final JButton backButton;

        public EndPanel() {
            // @qyx
            // TODO: 构建结束界面UI
            JPanel panel = new JPanel(null);
            panel.setBounds(framewidth / 2 - 400 / 2, 200, 400, 300);
            panel.setBackground(new Color(64, 76, 118));
            restartButton = new JButton(new ImageIcon("resources/img/new.jpg"));
            backButton = new JButton(new ImageIcon("resources/img/home.jpg"));
            resultLabel = new JLabel("游戏结束！");
            bestTimeLabel = new JLabel("");
            timeLabel = new JLabel("");
            // 设置布局
            setLayout(null);
            panel.setBounds(300, 100, 400, 300);
            resultLabel.setBounds(100, 100, 200, 50);
            bestTimeLabel.setBounds(100, 150, 200, 50);
            timeLabel.setBounds(100, 200, 200, 50);
            restartButton.setBounds(100, 250, 100, 50);
            backButton.setBounds(250, 250, 50, 50);
            // 设置组件
            this.add(panel);
            panel.add(resultLabel);
            panel.add(bestTimeLabel);
            panel.add(timeLabel);
            panel.add(restartButton);
            panel.add(backButton);
        }

        public void setTime(int seconds) {
            timeLabel.setText("你的时间: " + seconds + "秒");
        }

        public void setBestTime(boolean isBest, int bestTime) {
            if (gameEngine.getGameState() == GameState.WON) {
                bestTimeLabel.setText(isBest ? "新纪录！" : "你的最佳记录是: " + bestTime + "秒!");
                bestTimeLabel.setVisible(isBest);
            }
        }

        public void isWon() {
            if (gameEngine.getGameState() == GameState.WON) {
                resultLabel.setText("你赢了！");
            } else {
                resultLabel.setText("你输了！");
            }
        }

        public void setRestartListener(ActionListener listener) {
            restartButton.addActionListener(listener);
        }

        public void setBackListener(ActionListener listener) {
            backButton.addActionListener(listener);
        }
    }
}

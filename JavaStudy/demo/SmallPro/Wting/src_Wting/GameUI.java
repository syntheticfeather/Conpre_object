package Wting.src_Wting;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.event.ActionListener;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;

import Wting.src_Wting.Enums.GameState;

public class GameUI {

    private final JFrame mainFrame;
    private final CardLayout cardLayout;
    private final JPanel cardPanel;

    // 主要界面
    private final StartPanel startPanel;//主界面
    private final GamePanel gamePanel;
    private final PausePanel pausePanel;//暂停界面
    private final EndPanel endPanel;//失败界面

    //int framewidth;

    // 游戏引擎引用
    private final GameEngine gameEngine;
    private final BestScoreManager BestScoreManager;

    private String path = "Wting\\resources _Wting\\img";

    public GameUI(GameController gc, GameEngine gE, BestScoreManager bSM) {
        gameEngine = gE;
        BestScoreManager = bSM;
        mainFrame = new JFrame("消灭星星");
        mainFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        mainFrame.setSize(600, 700);
        //framewidth = mainFrame.getWidth();

        // 创建卡片布局
        cardLayout = new CardLayout();
        cardPanel = new JPanel(cardLayout);

        // 初始化各个界面
        startPanel = new StartPanel();
        gamePanel = new GamePanel(gc);
        pausePanel = new PausePanel();
        endPanel = new EndPanel();

        // 添加界面到卡片面板
        cardPanel.add(startPanel, "START");
        cardPanel.add(gamePanel, "GAME");
        cardPanel.add(pausePanel, "PAUSE");
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
        // 继续上一轮游戏
        startPanel.setContinueButtonListener(_ -> {
            cardLayout.show(cardPanel, "GAME");
        });
        
        //暂停游戏
        gamePanel.setPauseButtonListener(_ -> {
            cardLayout.show(cardPanel, "PAUSE");
        });

        // 暂停后返回游戏
        pausePanel.setBackToGameListener(_ -> {
            cardLayout.show(cardPanel, "GAME");
        });

        // 暂停后回到主菜单
        pausePanel.setBackToStartListener(_ -> {
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
    public void showEndScreen(int score, boolean isBestScore, int bestScore) {
        endPanel.setScore(score);
        endPanel.setBestScore(isBestScore, bestScore);
        endPanel.repaint();
        cardLayout.show(cardPanel, "END");
    }

    //显示暂停界面
    public void showPauseScreen() {
        cardLayout.show(cardPanel, "PAUSE");
    }
    //显示开始界面
    public void showStartScreen() {
        cardLayout.show(cardPanel, "START");
    }
    public GamePanel getGamePanel() {
        return gamePanel;
    }   
    // 内部类：开始界面
    class StartPanel extends JPanel {

        // 一个可以点击得按钮
        private final JButton startButton, continueButton;

        private final JPanel panel;
        private final JLabel bestScoreLabel,gameTitleLabel;

        public StartPanel() {
            setLayout(null);
            // 构建开始界面UI
            panel = new JPanel(null);
            gameTitleLabel = new JLabel("消灭星星");
            gameTitleLabel.setFont(new Font("Microsoft YaHei", Font.BOLD, 40));
            gameTitleLabel.setForeground(Color.WHITE);

            startButton = new JButton("开始游戏");
            continueButton = new JButton("继续游戏");
            bestScoreLabel = new JLabel("最高分:" + BestScoreManager.getBestScore(),JLabel.CENTER);
            bestScoreLabel.setFont(new Font("Microsoft YaHei", Font.BOLD, 20));
            bestScoreLabel.setForeground(Color.WHITE);
            // 布局
            this.setBackground((new Color(255, 255, 255)));
            panel.setBounds(50, 100, 450, 550);
            panel.setBackground((new Color(59, 78, 127)));

            bestScoreLabel.setBounds(125, 150, 200, 30);
            gameTitleLabel.setBounds(125, 50, 200, 60);
            startButton.setBounds(150, 250, 150, 40);
            continueButton.setBounds(150, 310, 150, 40);
            // 设置组件
            this.add(panel);
            panel.add(startButton);
            panel.add(continueButton);
            panel.add(bestScoreLabel);
            panel.add(gameTitleLabel);
        }

        public void setStartButtonListener(ActionListener listener) {
            startButton.addActionListener(listener);
        }
        public void setContinueButtonListener(ActionListener listener) {
            continueButton.addActionListener(listener);
        }
    }

    // 内部类：游戏界面
    class GamePanel extends JPanel {

        final int CELL_SIZE = 30;
        private final GameController gameController;
        private final JButton pauseButton;//暂停按钮
        private JButton[][] buttons;
        private JLabel gameLevelLabel;//关卡
        private JLabel targetLabel;//目标分
        private JLabel scoreLabel;//得分
        private JPanel topMenuPanel,gridPanel;
        private JLabel passLabel;

        public GamePanel(GameController gC) {
            gameController = gC;
            setLayout(new BorderLayout());
            // 构建游戏界面UI
            pauseButton=new JButton();
            pauseButton.setIcon(new ImageIcon("Wting\\resources _Wting\\img\\pause.jpg"));

            gameLevelLabel=new JLabel("第 "+gameEngine.getLevel()+"关",JLabel.CENTER);
            gameLevelLabel.setFont(new Font("Microsoft YaHei", Font.BOLD, 30));
            gameLevelLabel.setForeground(Color.BLACK);

            targetLabel = new JLabel("目标: " +gameEngine.getTarget(), JLabel.CENTER);
            targetLabel.setFont(new Font("Microsoft YaHei", Font.BOLD, 20));//
            targetLabel.setForeground(Color.BLACK);

            scoreLabel = new JLabel("得分: " +gameEngine.getScore(), JLabel.CENTER);
            scoreLabel.setFont(new Font("Microsoft YaHei", Font.BOLD, 20));//
            scoreLabel.setForeground(Color.BLACK);

            topMenuPanel = new JPanel(null);
            gridPanel = new JPanel(new GridLayout(10, 10));
            // 设置布局
            topMenuPanel.setPreferredSize(new Dimension(600, 100));
            topMenuPanel.setBackground(Color.WHITE);

            //垂直排列容器
            JPanel labelsPanel=new JPanel();
            labelsPanel.setLayout(new GridLayout(3,1,0,5));
            labelsPanel.setOpaque(false);
            labelsPanel.add(gameLevelLabel);
            labelsPanel.add(targetLabel);
            labelsPanel.add(scoreLabel);
            labelsPanel.setBounds(200,10,200,80);
             
            passLabel = new JLabel("恭喜通关！");
            passLabel.setFont(new Font("Microsoft YaHei", Font.BOLD, 20));
            passLabel.setForeground(Color.WHITE);
            passLabel.setBounds(200, 200, 200, 50);
            passLabel.setVisible(false);
            this.add(passLabel);

            pauseButton.setBounds(25, 25, 50, 50);

            topMenuPanel.add(pauseButton);
            topMenuPanel.add(labelsPanel);
            this.add(topMenuPanel, BorderLayout.NORTH);
            initGameBoard(gameEngine);
        }

        public void initGameBoard(GameEngine gameEngine) {
            // 根据游戏引擎初始化游戏网格
            // 构建游戏界面UI
            // 初始化
            gridPanel.removeAll();
            gridPanel.setLayout(new GridLayout(10, 10)); // 使用网格布局
            buttons = null;
            int rows =10;
            int cols =10;
            buttons = new JButton[rows][cols];
            // 设置网格面板大小（根据单元格数量和大小）
            gridPanel.setPreferredSize(new Dimension(CELL_SIZE * cols, CELL_SIZE * rows));
            gridPanel.setBackground(new Color(64, 76, 118));
            // 设置布局样式
            for (int i = 0; i < rows; i++) {
                for (int j = 0; j < cols; j++) {
                    // 设置图片
                    if(gameEngine.getStarState("color", i, j)==0){
                        buttons[i][j] = new JButton(new ImageIcon("Wting\\resources _Wting\\img\\0.jpg"));
                    }
                    if(gameEngine.getStarState("color", i, j)==1){
                        buttons[i][j] = new JButton(new ImageIcon("Wting\\resources _Wting\\img\\1.jpg"));
                    }
                    if(gameEngine.getStarState("color", i, j)==2){
                        buttons[i][j] = new JButton(new ImageIcon("Wting\\resources _Wting\\img\\2.jpg"));
                    }
                    if(gameEngine.getStarState("color", i, j)==3){
                        buttons[i][j] = new JButton(new ImageIcon("Wting\\resources _Wting\\img\\3.jpg"));
                    }
                    if(gameEngine.getStarState("color", i, j)==4){
                        buttons[i][j] = new JButton(new ImageIcon("Wting\\resources _Wting\\img\\4.jpg"));
                    }
                    final int r=i,c=j;
                    buttons[i][j].addActionListener(e -> {
                        gameController.starClick(r, c);
                    });
                    gridPanel.add(buttons[i][j]); 
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
            this.add(gridPanel, BorderLayout.CENTER);
        }

        public void updateGameBoard() {
            // 更新游戏网格显示
            for (int i = 0; i < buttons.length; i++) {
                for (int j = 0; j < buttons[i].length; j++) {
                    // 更新按钮显示
                    //已经消灭
                    if (gameEngine.getStarState("exist", i, j) == 0) {
                        buttons[i][j].setIcon(new ImageIcon("Wting\\resources _Wting\\img\\5.jpg"));
                    } 
                    //未消灭
                    else {
                        if(gameEngine.getStarState("color", i, j)==0){
                            buttons[i][j].setIcon(new ImageIcon("Wting\\resources _Wting\\img\\0.jpg"));
                        }
                        if(gameEngine.getStarState("color", i, j)==1){
                            buttons[i][j].setIcon(new ImageIcon("Wting\\resources _Wting\\img\\1.jpg"));
                         }
                        if(gameEngine.getStarState("color", i, j)==2){
                            buttons[i][j].setIcon(new ImageIcon("Wting\\resources _Wting\\img\\2.jpg"));
                        }
                        if(gameEngine.getStarState("color", i, j)==3){
                            buttons[i][j].setIcon(new ImageIcon("Wting\\resources _Wting\\img\\3.jpg"));
                        }
                        if(gameEngine.getStarState("color", i, j)==4){
                            buttons[i][j].setIcon(new ImageIcon("Wting\\resources _Wting\\img\\4.jpg"));
                        }
                    }
                }
            }
            // 更新得分和目标
            updateScore();
            updateTarget();
        }

        public void updateLevel() {
            gameLevelLabel.setText("第 "+gameEngine.getLevel()+"关");
        }

        public void showPassMessage() {
            if(gameEngine.getGameState() == GameState.PASS){
                passLabel.setVisible(true);
                new Thread(() -> {
                    try {
                        Thread.sleep(2000);
                        passLabel.setVisible(false);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                }).start();
            }
        }

        public void setPauseButtonListener(ActionListener listener) {
            pauseButton.addActionListener(listener);
        }

        public void updateScore() {
            int score = gameEngine.getScore();
            scoreLabel.setText("得分: " + score);
        }

        public void updateTarget() {
            int target = gameEngine.getTarget();
            targetLabel.setText("目标: " + target);
        }

        public JButton[][] getButtons() {
            return buttons;
        }
    }

    class PausePanel extends JPanel{
        private JButton backToStartButton;//返回主菜单按钮
        private JButton backToGameButton;//返回游戏按钮

        public PausePanel(){
            setLayout(null);
            backToStartButton = new JButton("返回主菜单");
            backToGameButton = new JButton("返回游戏");

            backToStartButton.setBounds(100,100,150,50);
            backToGameButton.setBounds(100,200,150,50);
            this.add(backToStartButton);
            this.add(backToGameButton);
        }

        public void setBackToStartListener(ActionListener listener) {
            backToStartButton.addActionListener(listener);
        }
        public void setBackToGameListener(ActionListener listener) {
            backToGameButton.addActionListener(listener);
        }
    }

// 内部类：结束界面
    class EndPanel extends JPanel {

        private final JLabel resultLabel;
        private JLabel scoreLabel, bestScoreLabel;
        private final JButton backButton;
        private final JButton backToMenuButton; // 返回主菜单按钮

        public EndPanel() {
            // 构建结束界面UI
            JPanel panel = new JPanel(null);
            backButton = new JButton(new ImageIcon(path + "home.jpg"));
            backToMenuButton = new JButton("返回主菜单"); // 新增按钮
            resultLabel = new JLabel("再接再厉！");
            scoreLabel = new JLabel("得分: " + gameEngine.getScore());
            bestScoreLabel = new JLabel("",JLabel.CENTER);
            bestScoreLabel.setFont(new Font("Microsoft YaHei", Font.BOLD, 20));
            // 设置布局
            setLayout(null);
            panel.setBounds(300,100,400,250);
            panel.setBackground(new Color(64, 76, 118));
            resultLabel.setBounds(100, 100, 200, 50);
            scoreLabel.setBounds(100, 100, 200, 50);
            backButton.setBounds(250, 150, 100, 50);
            backToMenuButton.setBounds(220, 180, 150, 40);
            // 设置组件
            
            this.add(panel);
            panel.add(resultLabel);
            panel.add(scoreLabel);
            panel.add(bestScoreLabel);
            panel.add(backButton);
            panel.add(backToMenuButton);
        }
        public void backToMenuButtonListener(ActionListener listener) {
            backToMenuButton.addActionListener(listener);
        }
        
        public void setScore(int score) {
            scoreLabel.setText("得分: " + score );
        }

        public void setBestScore(boolean isBest, int bestScore) {
            bestScoreLabel.setText(isBest ? "新纪录：" + bestScore :"最佳记录：" + bestScore );
        }
    }
}

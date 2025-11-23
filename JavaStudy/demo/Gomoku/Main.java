package Gomoku;

import javax.swing.SwingUtilities;

// 主类：启动程序
public class Main {
    public static void main(String[] args) {
        // 在EDT线程中启动Swing应用
        SwingUtilities.invokeLater(GameController::new);
    }
}
package MineSweeper.src;

import javax.swing.SwingUtilities;

public class main {

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            GameUI gameUI = new GameUI();
            GameController controller = new GameController(gameUI);
            // 注意：在实际代码中需要将控制器与UI绑定
        });
    }
}

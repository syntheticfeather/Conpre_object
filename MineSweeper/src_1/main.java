package MineSweeper.src_1;

import javax.swing.SwingUtilities;

public class main {

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            GameController controller = new GameController();
            // 注意：在实际代码中需要将控制器与UI绑定
        });
    }
}

package MineSweeper.src;

// File 4: BestTimeManager.java (最佳时间管理)
import MineSweeper.src.Enums.*;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Map;

public class BestTimeManager {

    private static final String FILE_NAME = "best_times.txt";
    // 格式:
    // 难度:时间(就以秒数记录)
    private Map<Difficulty, Integer> bestTimes;
    // 哈希表。按 键 取 值

    public BestTimeManager() {
        bestTimes = new HashMap<>();
        loadBestTimes();
    }

    // 从文件加载最佳时间
    private void loadBestTimes() {
        try (BufferedReader reader = new BufferedReader(new FileReader(FILE_NAME))) {
            // TODO
        } catch (IOException e) {
            // 文件不存在或读取错误，使用默认值
            System.out.println("无法读取最佳时间记录: " + e.getMessage());
        }
    }

    // 检查并保存最佳时间
    public boolean checkAndSaveBestTime(Difficulty difficulty, int time) {
        // TODO
    }

    // 保存最佳时间到文件
    private void saveBestTimes() {
        try (PrintWriter writer = new PrintWriter(FILE_NAME)) {
        } catch (IOException e) {
            System.err.println("保存最佳时间失败: " + e.getMessage());
        }
    }

    // 获取指定难度的最佳时间
    public int getBestTime(Difficulty difficulty) {
    }
}

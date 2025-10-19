package MineSweeper.src_1;

// File 4: BestTimeManager.java (最佳时间管理)
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Map;

import MineSweeper.src_1.Enums.Difficulty;
import MineSweeper.src_1.Enums.GameState;

// @zff
public class BestTimeManager {

    // 同级目录下的文件名
    private static final String FILE_NAME = ".\\MineSweeper\\src_1\\best_times.txt";
    // 格式:
    // 难度:时间(就以秒数记录)
    private final Map<Difficulty, Integer> bestTimes;
    // 哈希表。按 键 取 值

    public BestTimeManager() {
        bestTimes = new HashMap<>();
        loadBestTimes();
    }

    // 从文件加载最佳时间
    private void loadBestTimes() {
        try (BufferedReader reader = new BufferedReader(new FileReader(FILE_NAME))) {

            // 如果文件为空，则手动加入最大值
            if (reader.readLine() == null) {
                for (Difficulty difficulty : Difficulty.values()) {
                    bestTimes.put(difficulty, Integer.MAX_VALUE);
                }
            } else {
                String line;
                while ((line = reader.readLine()) != null) {
                    String[] s = line.split(":");
                    if (s.length == 2) {
                        Difficulty difficulty = Difficulty.valueOf(s[0]);
                        int time = Integer.parseInt(s[1]);
                        bestTimes.put(difficulty, time);
                    }
                }
            }
        } catch (IOException e) {
            // 文件不存在或读取错误，使用默认值
            System.out.println("无法读取最佳时间记录: " + e.getMessage());
        }
    }

    // 检查并保存最佳时间
    public boolean checkAndSaveBestTime(Difficulty difficulty, int time, GameState gameState) {
        // TODO
        int flag = 0;
        if (gameState == GameState.WON && bestTimes.getOrDefault(difficulty, Integer.MAX_VALUE) > time) {
            bestTimes.put(difficulty, time);
            flag = 1;
        }
        saveBestTimes();
        return flag == 1;
    }
    // 保存最佳时间到文件

    private void saveBestTimes() {
        try (PrintWriter writer = new PrintWriter(FILE_NAME)) {
            for (Difficulty dif : Difficulty.values()) {
                int t = bestTimes.getOrDefault(dif, 0);
                writer.println(dif.name() + ":" + t);
            }
        } catch (IOException e) {
            System.err.println("保存最佳时间失败: " + e.getMessage());
        }
    }

    // 获取指定难度的最佳时间
    public int getBestTime(Difficulty difficulty) {
        return bestTimes.getOrDefault(difficulty, -1);
    }
}

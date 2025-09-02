package MineSweeper.src;

// File 4: BestTimeManager.java (最佳时间管理)
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Map;

import MineSweeper.src.Enums.Difficulty;

// @zff

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
        for (Difficulty difficulty : Difficulty.values()) {
            bestTimes.put(difficulty, Integer.MAX_VALUE);
        }
        try (BufferedReader reader = new BufferedReader(new FileReader(FILE_NAME))) {
            String line;//可能为空
            while((line=reader.readLine())!=null){
                if(line.isEmpty()){
                    continue;
                }
                String[] parts=line.split(":");
                if(parts.length!=2){
                    continue;
                }
                Difficulty difficulty = Difficulty.valueOf(parts[0].trim());
                int time = Integer.parseInt(parts[1].trim());
                if(time>0){
                    bestTimes.put(difficulty, time);
                }
            }
        } catch (IOException e) {
            // 文件不存在或读取错误，使用默认值
            System.out.println("无法读取最佳时间记录: " + e.getMessage());
        }
    }

    // 检查并保存最佳时间
    public boolean checkAndSaveBestTime(Difficulty difficulty, int time) {
        if(time<=0){
            return false;
        }
        Integer currentBest = bestTimes.getOrDefault(difficulty, Integer.MAX_VALUE);//可能为空
        if(currentBest==null||time<currentBest){
            bestTimes.put(difficulty, time);
            saveBestTimes();
            return true;
        }
        return false;
    }

    // 保存最佳时间到文件
    private void saveBestTimes() {
        try (PrintWriter writer = new PrintWriter(FILE_NAME)) {
            for (Map.Entry<Difficulty, Integer> entry : bestTimes.entrySet()) {
                writer.println(entry.getKey() + ":" + entry.getValue());
            }
        } catch (IOException e) {
            System.err.println("保存最佳时间失败: " + e.getMessage());
        }
    }

    // 获取指定难度的最佳时间
    public int getBestTime(Difficulty difficulty) {
        return bestTimes.getOrDefault(difficulty, Integer.MAX_VALUE);
    }
}
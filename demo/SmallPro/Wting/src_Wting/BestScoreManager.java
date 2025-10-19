package Wting.src_Wting;

// File 4: BestScoreManager.java
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;

import Wting.src_Wting.Enums.GameState;

public class BestScoreManager {
    static String FILE_NAME = "Wting\\src_Wting\\Enums\\best_score.txt"; 

    private int bestScore;
    
    public BestScoreManager() {
        loadBestScores();
    }

    // 从文件加载最佳得分
    private void loadBestScores() {
        try (BufferedReader reader = new BufferedReader(new FileReader(FILE_NAME))) {
            String line = reader.readLine();
            if (line== null||line.trim().isEmpty()) { 
                bestScore=0;
            } else {
                bestScore=Integer.parseInt(line.trim());//除去空格
            }
        } catch (IOException e) {
            // 文件不存在或读取错误，使用默认值
            System.out.println("无法读取最佳得分记录: " + e.getMessage());
            bestScore = 0; 
        }
    }

    // 检查并保存最佳得分
    public void checkAndSaveBestScore( int score, GameState gameState) {
        if (gameState == GameState.LOST && bestScore<score) {
            bestScore=score;
            saveBestScores();
        }
    }
    private void saveBestScores() {
        try (PrintWriter writer = new PrintWriter(FILE_NAME)) {
            writer.println(bestScore);
        } catch (IOException e) {
            System.err.println("保存最佳得分失败: " + e.getMessage());
        }
    }
    public int getBestScore() {
        return bestScore;
    }
}

package Gomoku;

import java.util.Stack;

import Gomoku.Enums.GameState;
import Gomoku.Enums.PieceType;

// 棋盘状态管理、落子验证、胜负判断
public class GameEngine {
    private static class Move {  //记录落子历史
        int row;
        int col;
        PieceType player;
        GameState previousState;
        Move(int row, int col, PieceType player, GameState previousState) {
            this.row = row;
            this.col = col;    
            this.player = player;
            this.previousState = previousState;
        }
    }
    private final Stack<Move> moveHistory = new Stack<>();
    private final int BOARD_SIZE;  // 棋盘大小（默认15x15）
    private PieceType[][] board;   // 棋盘状态
    private GameState gameState;   // 当前游戏状态
    private PieceType currentPlayer;  // 当前回合玩家

    public GameEngine() {
        this.BOARD_SIZE = 15;
        initGame();
    }

    // 初始化游戏
    public void initGame() {
        board = new PieceType[BOARD_SIZE][BOARD_SIZE];
        for (int i = 0; i < BOARD_SIZE; i++) {
            for (int j = 0; j < BOARD_SIZE; j++) {
                board[i][j] = PieceType.EMPTY;
            }
        }
        gameState = GameState.PLAYING;
        currentPlayer = PieceType.BLACK; // 黑棋先行
        moveHistory.clear();
    }

    // 落子（返回是否落子成功）
    public boolean placePiece(int row, int col) {
        // 检查位置是否合法（在棋盘内且为空位）
        if (row < 0 || row >= BOARD_SIZE || col < 0 || col >= BOARD_SIZE) {
            return false;
        }
        if (board[row][col] != PieceType.EMPTY || gameState != GameState.PLAYING) {
            return false;
        }

        moveHistory.push(new Move(row, col, currentPlayer, gameState));
        board[row][col] = currentPlayer;
        checkGameState(row, col);
        currentPlayer = (currentPlayer == PieceType.BLACK) ? PieceType.WHITE : PieceType.BLACK;
        return true;
    }

    // 悔棋功能
    public boolean undoMove() {
        if (moveHistory.isEmpty()) {
            return false;  // 没有可悔的步骤
        }

        Move lastMove = moveHistory.pop();
        board[lastMove.row][lastMove.col] = PieceType.EMPTY;
        currentPlayer = lastMove.player;
        gameState = lastMove.previousState;
        
        return true;
    }

    // 检查游戏状态（胜负/平局）
    private void checkGameState(int row, int col) {
        PieceType winner = checkWin(row, col);
        if (winner == PieceType.BLACK) {
            gameState = GameState.BLACK_WIN;
        } else if (winner == PieceType.WHITE) {
            gameState = GameState.WHITE_WIN;
        } else if (isBoardFull()) {
            gameState = GameState.DRAW;
        }
    }

    // 判断当前落子是否形成五连子
    private PieceType checkWin(int row, int col) {
        PieceType target = board[row][col];
        int[][] directions = {{0, 1}, {1, 0}, {1, 1}, {1, -1}};//检查方向

        for (int[] dir : directions) {
            int count = 1;
            // 正向计数
            for (int i = 1; i < 5; i++) {
                int newRow = row + dir[0] * i;
                int newCol = col + dir[1] * i;
                if (isValidPosition(newRow, newCol) && board[newRow][newCol] == target) {
                    count++;
                } else {
                    break;
                }
            }
            // 反向计数
            for (int i = 1; i < 5; i++) {
                int newRow = row - dir[0] * i;
                int newCol = col - dir[1] * i;
                if (isValidPosition(newRow, newCol) && board[newRow][newCol] == target) {
                    count++;
                } else {
                    break;
                }
            }
            if (count >= 5) {
                return target;  // 五连子，返回获胜方
            }
        }
        return PieceType.EMPTY;  // 未获胜
    }

    // 检查棋盘是否下满（平局）
    private boolean isBoardFull() {
        for (int i = 0; i < BOARD_SIZE; i++) {
            for (int j = 0; j < BOARD_SIZE; j++) {
                if (board[i][j] == PieceType.EMPTY) {
                    return false;
                }
            }
        }
        return true;
    }

    // 检查坐标是否在棋盘内
    private boolean isValidPosition(int row, int col) {
        return row >= 0 && row < BOARD_SIZE && col >= 0 && col < BOARD_SIZE;
    }

    // 获取棋盘边界大小
    public int getBoardSize() {
        return BOARD_SIZE;
    }

    // 获取棋子状态
    public PieceType getPiece(int row, int col) {
        return board[row][col];
    }

    // 获取游戏状态
    public GameState getGameState() {
        return gameState;
    }

    // 获取当前的下棋方
    public PieceType getCurrentPlayer() {
        return currentPlayer;
    }
}
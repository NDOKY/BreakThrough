import java.util.ArrayList;
import java.util.List;

/**
 * Breakthrough board: 8x8 grid. Row 0 = rank 1 (black back rank), row 7 = rank 8 (red back rank).
 * Compatible with Client's int[column][row] layout when loading from server.
 */
public class Board {

    private static final int GRID_SIZE = 8;
    private static final int RED_GOAL_ROW = 0;
    private static final int BLACK_GOAL_ROW = 7;

    // Internal grid: grid[rowIndex][colIndex], row 0 = rank 1, row 7 = rank 8.
    private Mark[][] grid;

    public Board() {

        grid = new Mark[GRID_SIZE][GRID_SIZE];
        initializeDefaultPosition();
    }

    /**
     * Build board from server state. Server sends values in column-major order:
     * board[column][row], row 0 = rank 1 (black back), row 7 = rank 8 (red back).
     */
    public Board(int[][] serverBoard) {

        grid = new Mark[GRID_SIZE][GRID_SIZE];

        for (int rowIndex = 0; rowIndex < GRID_SIZE; rowIndex++) {

            for (int colIndex = 0; colIndex < GRID_SIZE; colIndex++) {

                grid[rowIndex][colIndex] = Mark.fromServerCode(serverBoard[colIndex][rowIndex]);
            }
        }
    }

    // Copy constructor for search.
    public Board(Board other) {

        grid = new Mark[GRID_SIZE][GRID_SIZE];

        for (int rowIndex = 0; rowIndex < GRID_SIZE; rowIndex++) {

            for (int colIndex = 0; colIndex < GRID_SIZE; colIndex++) {

                grid[rowIndex][colIndex] = other.grid[rowIndex][colIndex];
            }
        }
    }

    private void initializeDefaultPosition() {

        for (int rowIndex = 0; rowIndex < GRID_SIZE; rowIndex++) {

            for (int colIndex = 0; colIndex < GRID_SIZE; colIndex++) {

                grid[rowIndex][colIndex] = Mark.vide;
            }
        }

        for (int colIndex = 0; colIndex < GRID_SIZE; colIndex++) {

            grid[6][colIndex] = Mark.rouge;
            grid[7][colIndex] = Mark.rouge;
        }

        for (int colIndex = 0; colIndex < GRID_SIZE; colIndex++) {

            grid[0][colIndex] = Mark.noir;
            grid[1][colIndex] = Mark.noir;
        }
    }

    // Apply a move in server format ("D6-D5" or "D6D5"). Does not validate.
    public void makeMove(String move) {

        if (move == null || move.length() < 4) {

            return;
        }

        String normalized = move.replace("-", "").trim();

        if (normalized.length() < 4) {

            return;
        }

        int fromColIndex = normalized.charAt(0) - 'A';
        int fromRank = Character.getNumericValue(normalized.charAt(1));
        int toColIndex = normalized.charAt(2) - 'A';
        int toRank = Character.getNumericValue(normalized.charAt(3));
        int fromRowIndex = 8 - fromRank;
        int toRowIndex = 8 - toRank;

        if (fromColIndex < 0 || fromColIndex >= GRID_SIZE || fromRowIndex < 0 || fromRowIndex >= GRID_SIZE
                || toColIndex < 0 || toColIndex >= GRID_SIZE || toRowIndex < 0 || toRowIndex >= GRID_SIZE) {
                    
            return;
        }
        
        Mark piece = grid[fromRowIndex][fromColIndex];
        grid[toRowIndex][toColIndex] = piece;
        grid[fromRowIndex][fromColIndex] = Mark.vide;
    }

    // Returns true if the move is valid for the given side.
    public boolean isValidMove(String move, Mark sideToMove) {
        
        if (move == null || move.length() < 4) {
            return false;
        }

        String normalized = move.replace("-", "").trim();

        if (normalized.length() < 4) {
            return false;
        }
        
        int fromColIndex = normalized.charAt(0) - 'A';
        int fromRank = Character.getNumericValue(normalized.charAt(1));
        int toColIndex = normalized.charAt(2) - 'A';
        int toRank = Character.getNumericValue(normalized.charAt(3));
        int fromRowIndex = 8 - fromRank;
        int toRowIndex = 8 - toRank;

        if (fromColIndex < 0 || fromColIndex >= GRID_SIZE || fromRowIndex < 0 || fromRowIndex >= GRID_SIZE
                || toColIndex < 0 || toColIndex >= GRID_SIZE || toRowIndex < 0 || toRowIndex >= GRID_SIZE) {

            return false;
        }

        if (grid[fromRowIndex][fromColIndex] != sideToMove) {
            return false;
        }

        if (grid[toRowIndex][toColIndex] == sideToMove) {
            return false;
        }

        int rowDelta = toRowIndex - fromRowIndex;
        int colDelta = Math.abs(toColIndex - fromColIndex);

        if (sideToMove == Mark.rouge) {

            if (rowDelta >= 0) {
                return false;
            }

            if (rowDelta != -1) {
                return false;
            }

            if (colDelta == 0) {
                return grid[toRowIndex][toColIndex] == Mark.vide;
            }

            if (colDelta == 1) {
                return true;
            }

            return false;

        } else {

            if (rowDelta <= 0) {
                return false;
            }

            if (rowDelta != 1) {
                return false;
            }

            if (colDelta == 0) {
                return grid[toRowIndex][toColIndex] == Mark.vide;
            }

            if (colDelta == 1) {
                return true;
            }

            return false;
        }
    }

    // All legal moves for the given side. Each move is in format "A2A3" (no dash).
    public List<String> generateAllMoves(Mark sideToMove) {

        List<String> moves = new ArrayList<>();
        int forwardDirection = (sideToMove == Mark.rouge) ? -1 : 1;

        for (int rowIndex = 0; rowIndex < GRID_SIZE; rowIndex++) {

            for (int colIndex = 0; colIndex < GRID_SIZE; colIndex++) {

                if (grid[rowIndex][colIndex] != sideToMove) {
                    continue;
                }

                int nextRow = rowIndex + forwardDirection;

                if (nextRow < 0 || nextRow >= GRID_SIZE) {
                    continue;
                }

                int fromRank = 8 - rowIndex;
                char fromCol = (char) ('A' + colIndex);

                if (grid[nextRow][colIndex] == Mark.vide) {
                    moves.add(moveString(fromCol, fromRank, (char) ('A' + colIndex), 8 - nextRow));
                }

                if (colIndex - 1 >= 0) {

                    Mark target = grid[nextRow][colIndex - 1];

                    if (target == Mark.vide || target != sideToMove) {
                        moves.add(moveString(fromCol, fromRank, (char) ('A' + colIndex - 1), 8 - nextRow));
                    }
                }
                if (colIndex + 1 < GRID_SIZE) {

                    Mark target = grid[nextRow][colIndex + 1];

                    if (target == Mark.vide || target != sideToMove) {
                        moves.add(moveString(fromCol, fromRank, (char) ('A' + colIndex + 1), 8 - nextRow));
                    }
                }
            }
        }
        return moves;
    }

    private static String moveString(char fromCol, int fromRank, char toCol, int toRank) {

        return "" + fromCol + fromRank + toCol + toRank;
    }

    // Apply move and return new board. Current board remains unchanged.
    public Board applyMove(String move) {

        Board copy = new Board(this);
        copy.makeMove(move);
        return copy;
    }

    public Mark getCell(int rowIndex, int colIndex) {

        if (rowIndex < 0 || rowIndex >= GRID_SIZE || colIndex < 0 || colIndex >= GRID_SIZE) {
            return Mark.vide;
        }
        
        return grid[rowIndex][colIndex];
    }

    // Returns the winner (rouge or noir) if a piece reached the goal row. Otherwise null.
    public Mark getWinner() {

        for (int colIndex = 0; colIndex < GRID_SIZE; colIndex++) {

            if (grid[RED_GOAL_ROW][colIndex] == Mark.rouge) {
                return Mark.rouge;
            }

            if (grid[BLACK_GOAL_ROW][colIndex] == Mark.noir) {
                return Mark.noir;
            }
        }

        return null;
    }

    public boolean isGameOver() {
        return getWinner() != null;
    }

    public static int getGridSize() {
        return GRID_SIZE;
    }

    // Export to Client-style int[column][row] (0, 2, 4).
    public int[][] toServerBoard() {

        int[][] serverBoard = new int[GRID_SIZE][GRID_SIZE];

        for (int rowIndex = 0; rowIndex < GRID_SIZE; rowIndex++) {

            for (int colIndex = 0; colIndex < GRID_SIZE; colIndex++) {

                serverBoard[colIndex][rowIndex] = grid[rowIndex][colIndex].toServerCode();
            }
        }

        return serverBoard;
    }
}

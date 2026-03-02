public class Board {

    private Mark[][] board;
    private static Mark player = Mark.rouge;
    private static Mark opponent = Mark.noir;
    private static Mark empty = Mark.vide;
    private final int GRID_SIZE = 8;

    public Board() {
        initializeBoard();
    }

    private void initializeBoard(){
        this.board = new Mark[GRID_SIZE][GRID_SIZE];

        for (int i = 0; i < GRID_SIZE; i++) {
            for (int j = 0; j < GRID_SIZE; j++) {
                this.board[i][j] = Mark.vide;
            }
        }
        
        // Place red marks on rows 1-2 (array indices 6-7)
        for (int j = 0; j < GRID_SIZE; j++) {
            board[6][j] = Mark.rouge;
            board[7][j] = Mark.rouge;
        }
        
        // Place black marks on rows 7-8 (array indices 0-1)
        for (int j = 0; j < GRID_SIZE; j++) {
            board[0][j] = Mark.noir;
            board[1][j] = Mark.noir;
        }
    }

    public void makeMove(String move) {
        // Parse move string (e.g., "A7A6" or "A7-A6")
        if (move == null || move.length() < 4) {
            return;
        }
        
        // Remove dashes to handle both formats
        move = move.replace("-", "");
        if (move.length() < 4) {
            return;
        }
        
        // Extract source and destination
        char fromCol = move.charAt(0);
        int fromRow = Character.getNumericValue(move.charAt(1));
        char toCol = move.charAt(2);
        int toRow = Character.getNumericValue(move.charAt(3));
        
        // Convert chess notation to array indices
        int fromColIndex = fromCol - 'A';
        int fromRowIndex = 8 - fromRow;
        int toColIndex = toCol - 'A';
        int toRowIndex = 8 - toRow;
        
        // Validate indices
        if (fromColIndex < 0 || fromColIndex >= GRID_SIZE || fromRowIndex < 0 || fromRowIndex >= GRID_SIZE ||
            toColIndex < 0 || toColIndex >= GRID_SIZE || toRowIndex < 0 || toRowIndex >= GRID_SIZE) {
            return;
        }
        
        // Move the piece
        Mark piece = board[fromRowIndex][fromColIndex];
        board[toRowIndex][toColIndex] = piece;
        board[fromRowIndex][fromColIndex] = Mark.vide;
    }

    public String findSimpleMove() {
        // Find the first opponent (black) piece and move it forward
        for (int i = 0; i < GRID_SIZE; i++) {
            for (int j = 0; j < GRID_SIZE; j++) {
                if (board[i][j] == opponent) {
                    // Try to move forward (increase row index)
                    if (i + 1 < GRID_SIZE && board[i + 1][j] == Mark.vide) {
                        // Convert array indices back to chess notation
                        char fromCol = (char) ('A' + j);
                        int fromRow = 8 - i;
                        char toCol = (char) ('A' + j);
                        int toRow = 8 - (i + 1);

                        // Make the move on the board
                        board[i + 1][j] = opponent;
                        board[i][j] = Mark.vide;

                        System.out.println("Moving from " + fromCol + "" + fromRow + toCol + "" + toRow);
                        
                        return fromCol + "" + fromRow + toCol + "" + toRow;
                    }
                }
            }
        }
        // No valid move found
        return null;
    }

    
}

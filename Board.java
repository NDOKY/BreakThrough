public class Board {

    static Mark[][] localBoard;
    private static Mark player = Mark.rouge;
    private static Mark opponent = Mark.noir;
    private static Mark empty = Mark.vide;
    private final int GRID_SIZE = 8;

    public Board() {
        initializeBoard();
    }

    private void initializeBoard(){
        this.localBoard = new Mark[GRID_SIZE][GRID_SIZE];

        for (int i = 0; i < GRID_SIZE; i++) {
            for (int j = 0; j < GRID_SIZE; j++) {
                this.localBoard[i][j] = Mark.vide;
            }
        }
        
        // Place red marks on rows 1-2 (array indices 6-7)
        for (int j = 0; j < GRID_SIZE; j++) {
            localBoard[6][j] = Mark.rouge;
            localBoard[7][j] = Mark.rouge;
        }
        
        // Place black marks on rows 7-8 (array indices 0-1)
        for (int j = 0; j < GRID_SIZE; j++) {
            localBoard[0][j] = Mark.noir;
            localBoard[1][j] = Mark.noir;
        }
    }

    /* public void makeMove(String move) {
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
    } */

    public String findSimpleMove() {
        // PRIORITY 1: Find ALL opponent pieces capable of diagonal capture
        String captureMove = findDiagonalCapture();
        if (captureMove != null) {
            System.out.println("Found capture move: " + captureMove);
            return captureMove;
        }
        
        // PRIORITY 2: Find a forward move if no captures available
        String forwardMove = findForwardMove();
        return forwardMove;
    }
    
    public String findDiagonalCapture() {
        // Check all opponent pieces to find a capture move
        for (int i = 0; i < GRID_SIZE; i++) {
            for (int j = 0; j < GRID_SIZE; j++) {
                if (localBoard[i][j] == opponent) {
                    // Black pieces move downward (increasing row index)
                    // Check diagonal-left capture (must capture a player piece)
                    if (i + 1 < GRID_SIZE && j - 1 >= 0 && localBoard[i + 1][j - 1] == player) {
                        char fromCol = (char) ('A' + j);
                        int fromRow = 8 - i;
                        char toCol = (char) ('A' + (j - 1));
                        int toRow = 8 - (i + 1);

                        localBoard[i + 1][j - 1] = opponent;
                        localBoard[i][j] = Mark.vide;

                        String move = fromCol + "" + fromRow + toCol + "" + toRow;
                        System.out.println("Capturing from " + move);
                        System.out.println("Board after AI capture move:");
                        printBoard();
                        return move;
                    }

                    // Check diagonal-right capture (must capture a player piece)
                    if (i + 1 < GRID_SIZE && j + 1 < GRID_SIZE && localBoard[i + 1][j + 1] == player) {
                        char fromCol = (char) ('A' + j);
                        int fromRow = 8 - i;
                        char toCol = (char) ('A' + (j + 1));
                        int toRow = 8 - (i + 1);

                        localBoard[i + 1][j + 1] = opponent;
                        localBoard[i][j] = Mark.vide;

                        String move = fromCol + "" + fromRow + toCol + "" + toRow;
                        System.out.println("Capturing from " + move);
                        System.out.println("Board after AI capture move:");
                        printBoard();
                        return move;
                    }
                }
            }
        }
        return null;
    }
    
    private String findForwardMove() {
        // Find a forward move if no captures available
        for (int i = 0; i < GRID_SIZE; i++) {
            for (int j = 0; j < GRID_SIZE; j++) {
                if (localBoard[i][j] == opponent) {
                    // Black pieces move downward (increasing row index)
                    if (i + 1 < GRID_SIZE && localBoard[i + 1][j] == Mark.vide) {
                        char fromCol = (char) ('A' + j);
                        int fromRow = 8 - i;
                        char toCol = (char) ('A' + j);
                        int toRow = 8 - (i + 1);

                        localBoard[i + 1][j] = opponent;
                        localBoard[i][j] = Mark.vide;

                        System.out.println("Moving from " + fromCol + "" + fromRow + toCol + "" + toRow);
                        
                        return fromCol + "" + fromRow + toCol + "" + toRow;
                    }
                }
            }
        }
        return null;
    }
    
    public void printBoard() {
        System.out.println("  A B C D E F G H");
        System.out.println("  ---------------");
        for (int i = 0; i < 8; i++) {
            System.out.print((8 - i) + "|");
            for (int j = 0; j < 8; j++) {
                char piece = ' ';
                if (localBoard[i][j] == Mark.rouge) {
                    piece = 'R';
                } else if (localBoard[i][j] == Mark.noir) {
                    piece = 'N';
                }
                System.out.print(piece + "|");
            }
            System.out.println(" " + (8 - i));
        }
        System.out.println("  ---------------");
        System.out.println("  A B C D E F G H");
    }

    public void applyMoveToBoard(String move) {
if (move == null) return;

    // 1. Normalize: Uppercase and remove all non-alphanumeric chars (handles A7-A6, A7 A6, etc.)
    move = move.toUpperCase().replaceAll("[^A-Z0-String0-9]", "");

    if (move.length() < 4) return;

    // 2. Extract and convert
    int fromColIndex = move.charAt(0) - 'A';
    int fromRowIndex = 8 - Character.getNumericValue(move.charAt(1));
    int toColIndex = move.charAt(2) - 'A';
    int toRowIndex = 8 - Character.getNumericValue(move.charAt(3));

    // 3. Validate Bounds
    if (isOutOfBounds(fromColIndex, fromRowIndex) || isOutOfBounds(toColIndex, toRowIndex)) {
        System.out.println("Invalid move: Out of board bounds.");
        return;
    }

    // 4. Validate Source (Is there a piece to move?)
    Mark piece = localBoard[fromRowIndex][fromColIndex];
    if (piece == Mark.vide) {
        System.out.println("Invalid move: Source square is empty.");
        return;
    }

    // 5. Execute Move
    localBoard[toRowIndex][toColIndex] = piece;
    localBoard[fromRowIndex][fromColIndex] = Mark.vide;
}

private boolean isOutOfBounds(int col, int row) {
    return col < 0 || col >= GRID_SIZE || row < 0 || row >= GRID_SIZE;
    }

}

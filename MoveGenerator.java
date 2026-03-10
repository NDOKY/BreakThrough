/**
 * Generates the next move for the AI. Keeps board state in sync with the server:
 * setBoard when the game starts (message 1 or 2); nextMove applies the opponent's move and returns our move.
 */
public class MoveGenerator {

    private Board board;
    private final boolean playingRed;
    private static final long TIME_LIMIT_MS = 50;

    public MoveGenerator(boolean playingRed) {
        this.playingRed = playingRed;
        this.board = new Board();
    }

    /**
     * Set the board from the server state (int[column][row], values 0/2/4).
     * Call this when receiving message "1" (red) or "2" (black) with the board data.
     */
    public void setBoard(int[][] serverBoard) {
        this.board = new Board(serverBoard);
    }

    /**
     * Apply the last opponent move (from message "3"), then compute and return our move.
     * Validates the opponent move as required by the PDF; if invalid, still returns a legal move.
     */
    public String nextMove(String lastOpponentMove) {

        Mark opponentSide = playingRed ? Mark.noir : Mark.rouge;

        if (lastOpponentMove != null && !lastOpponentMove.trim().isEmpty() && !isInvalidMovePlaceholder(lastOpponentMove)) {

            if (board.isValidMove(lastOpponentMove.trim(), opponentSide)) {
                board.makeMove(lastOpponentMove.trim());
            }
        }

        if (board.isGameOver()) {
            return null;
        }

        Mark ourSide = playingRed ? Mark.rouge : Mark.noir;
        String ourMove = GameAI.getBestMove(board, ourSide, TIME_LIMIT_MS);

        if (ourMove != null) {
            board.makeMove(ourMove);
        }

        return ourMove;
    }

    private static boolean isInvalidMovePlaceholder(String move) {

        String normalized = move.replace("-", "").trim();
        return normalized.length() >= 4 && normalized.charAt(0) == normalized.charAt(2) && normalized.charAt(1) == normalized.charAt(3);
    }
}

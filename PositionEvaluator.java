/**
 * Static evaluation of a Breakthrough position. Positive = good for red, negative = good for black.
 * Used by minimax/alpha-beta. Detects wins.
 */
public final class PositionEvaluator {

    private static final int WIN_SCORE = 100_000;
    private static final int PIECE_VALUE = 100;
    private static final int ADVANCE_BONUS = 10;

    private PositionEvaluator() {
    }

    /**
     * Evaluate the position for minimax. Positive favors rouge, negative favors noir.
     * If the position is a win for a side, returns +/- WIN_SCORE.
     */
    public static int evaluate(Board board, Mark sideToMove) {
        Mark winner = board.getWinner();
        if (winner == Mark.rouge) {
            return WIN_SCORE;
        }
        if (winner == Mark.noir) {
            return -WIN_SCORE;
        }

        int score = 0;
        for (int rowIndex = 0; rowIndex < Board.getGridSize(); rowIndex++) {
            for (int colIndex = 0; colIndex < Board.getGridSize(); colIndex++) {
                Mark cell = board.getCell(rowIndex, colIndex);
                if (cell == Mark.rouge) {
                    score += PIECE_VALUE;
                    score += (7 - rowIndex) * ADVANCE_BONUS;
                } else if (cell == Mark.noir) {
                    score -= PIECE_VALUE;
                    score -= rowIndex * ADVANCE_BONUS;
                }
            }
        }
        return score;
    }
}

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class GameAITest {

    @Test
    void bestMoveFromOpeningIsLegalForRed() {
        Board board = new Board();
        String move = GameAI.getBestMove(board, Mark.rouge, 800L);
        assertNotNull(move);
        assertTrue(board.isValidMove(move, Mark.rouge));
    }

    @Test
    void bestMoveFromOpeningIsLegalForBlack() {
        Board board = new Board();
        board.makeMove("A1A2");
        String move = GameAI.getBestMove(board, Mark.noir, 800L);
        assertNotNull(move);
        assertTrue(board.isValidMove(move, Mark.noir));
    }
}

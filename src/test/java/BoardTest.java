import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class BoardTest {

    @Test
    void newBoardHasNoWinner() {
        assertNull(new Board().getWinner());
    }

    @Test
    void initialPieceCount() {
        Board board = new Board();
        int rouge = 0;
        int noir = 0;
        for (int rowIndex = 0; rowIndex < 8; rowIndex++) {
            for (int colIndex = 0; colIndex < 8; colIndex++) {
                if (board.getCell(rowIndex, colIndex) == Mark.rouge) {
                    rouge++;
                }
                if (board.getCell(rowIndex, colIndex) == Mark.noir) {
                    noir++;
                }
            }
        }
        assertEquals(16, rouge);
        assertEquals(16, noir);
    }

    @Test
    void rougeHasLegalMovesFromStandardStart() {
        Board board = new Board();
        assertFalse(board.generateAllMoves(Mark.rouge).isEmpty());
        String move = board.generateAllMoves(Mark.rouge).get(0);
        assertNotNull(move);
        assertTrue(board.isValidMove(move, Mark.rouge));
    }

    @Test
    void noirHasLegalMovesFromStandardStart() {
        Board board = new Board();
        assertFalse(board.generateAllMoves(Mark.noir).isEmpty());
        String move = board.generateAllMoves(Mark.noir).get(0);
        assertTrue(board.isValidMove(move, Mark.noir));
    }

    @Test
    void rejectedMoveFromWrongPieceSquare() {
        Board board = new Board();
        assertFalse(board.isValidMove("H8H7", Mark.rouge));
    }

    @Test
    void copyConstructorMatchesOriginal() {
        Board original = new Board();
        Board copy = new Board(original);
        for (int rowIndex = 0; rowIndex < 8; rowIndex++) {
            for (int colIndex = 0; colIndex < 8; colIndex++) {
                assertEquals(original.getCell(rowIndex, colIndex), copy.getCell(rowIndex, colIndex));
            }
        }
    }
}

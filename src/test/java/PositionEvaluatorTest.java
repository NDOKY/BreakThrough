import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class PositionEvaluatorTest {

    @Test
    void openingIsSymmetricInEvaluator() {
        Board board = new Board();
        assertEquals(0, PositionEvaluator.evaluate(board));
    }
}

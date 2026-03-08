public class MoveGenerator {

    Board board;
    
    public MoveGenerator() {

        this.board = new Board();
    }

    public String nextMove(String lastMove) {
        // Update board with the last move received (could be from player or opponent)
        if (lastMove != null && !lastMove.isEmpty()) {
            board.applyMoveToBoard(lastMove);
            //Board.printBoard();
            System.out.println("Applied received move to board: " + lastMove);
        }
        
        // Find the next move based on current board state
        return board.findSimpleMove();
    }   
}
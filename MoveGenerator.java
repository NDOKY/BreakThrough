public class MoveGenerator {

    Board board;
    
    public MoveGenerator() {

        this.board = new Board();
    }

    public String nextMove(String ennemyMove) {
        // Update board with enemy's move
        if (ennemyMove != null && !ennemyMove.isEmpty()) {
            board.makeMove(ennemyMove);
        }

        // TODO Auto-generated method stub
        
        return board.findSimpleMove();
    }   
}
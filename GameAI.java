import java.util.List;

public final class GameAI {

    private GameAI() {
    }

    public static String getBestMove(Board board, Mark sideToMove, long limitMs) {

        if (board.isGameOver()) {

            return null;
        }

        List<String> moves = board.generateAllMoves(sideToMove);

        if (moves.isEmpty()) {

            return null;
        }
        if (moves.size() == 1) {

            return moves.get(0);
        }

        int depth = 1;
        long deadline = System.currentTimeMillis() + limitMs;
        String bestMove = moves.get(0);
        boolean[] timedOut = new boolean[1];

        while (System.currentTimeMillis() < deadline) {

            int alpha = Integer.MIN_VALUE;
            int beta = Integer.MAX_VALUE;
            int depthBestScore = sideToMove == Mark.rouge ? Integer.MIN_VALUE : Integer.MAX_VALUE;
            String depthBestMove = null;
            timedOut[0] = false;

            for (String move : moves) {

                if (System.currentTimeMillis() >= deadline) {

                    break;
                }

                Board child = board.applyMove(move);
                int score = alphaBeta(child, depth, alpha, beta, opposite(sideToMove), deadline, timedOut);

                if (timedOut[0]) {

                    break;
                }
                
                if (sideToMove == Mark.rouge) {
                    
                    if (score > depthBestScore) {

                        depthBestScore = score;
                        depthBestMove = move;
                    }

                    alpha = Math.max(alpha, depthBestScore);

                } else {

                    if (score < depthBestScore) {

                        depthBestScore = score;
                        depthBestMove = move;
                    }
                    
                    beta = Math.min(beta, depthBestScore);
                }

                if (beta <= alpha) {

                    break;
                }
                
            }

            if (timedOut[0]) {

                break;
            }

            if (depthBestMove != null) {

                bestMove = depthBestMove;
            }

            depth++;
        }

        return bestMove;
    }

    private static int alphaBeta(
            Board board,
            int depth,
            int alpha,
            int beta,
            Mark currentPlayer,
            long deadline,
            boolean[] timedOut) {

        if (System.currentTimeMillis() >= deadline) {

            timedOut[0] = true;
            return 0;
        }

        Mark winner = board.getWinner();

        if (winner != null) {

            return PositionEvaluator.evaluate(board);
        }

        if (depth == 0) {

            return PositionEvaluator.evaluate(board);
        }

        List<String> moves = board.generateAllMoves(currentPlayer);

        if (moves.isEmpty()) {

            return PositionEvaluator.evaluate(board);
        }

        if (currentPlayer == Mark.rouge) {

            int value = Integer.MIN_VALUE;

            for (String move : moves) {

                if (timedOut[0]) {

                    break;
                }

                Board child = board.applyMove(move);
                value = Math.max(value, alphaBeta(child, depth - 1, alpha, beta,
                        opposite(currentPlayer), deadline, timedOut));

                alpha = Math.max(alpha, value);

                if (beta <= alpha) {

                    break;
                }
            }

            return value;

        }

        int value = Integer.MAX_VALUE;

        for (String move : moves) {

            if (timedOut[0]) {

                break;
            }

            Board child = board.applyMove(move);
            value = Math.min(value, alphaBeta(child, depth - 1, alpha, beta,
                    opposite(currentPlayer), deadline, timedOut));

            beta = Math.min(beta, value);

            if (beta <= alpha) {
                
                break;

            }
        }

        return value;
    }

    private static Mark opposite(Mark mark) {

        return mark == Mark.rouge ? Mark.noir : Mark.rouge;
    }

}

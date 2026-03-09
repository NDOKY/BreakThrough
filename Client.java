import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.net.Socket;

public class Client {
    /** If true, send ranks as 9-rank so server (rank 1 = top) gets correct squares. */
    private static final boolean SERVER_RANK_1_IS_TOP = false;
    /** Time limit per move in ms. Set from command line to match game minuterie; default 5 sec. */
    private static long timeLimitMs = 4_900;

    public static void main(String[] args) {

        Socket myClient = null;
        BufferedInputStream input = null;
        BufferedOutputStream output = null;
        int[][] board = new int[8][8];
        Board gameBoard = null;
        /** Snapshot of board before we send our move; restored when server rejects (cmd 4). */
        Board boardBeforeOurMove = null;
        /** Last move we sent (so we can try a different one when server rejects with cmd 4). */
        String lastSentMove = null;
        Mark ourSide = null;

        if (args.length > 0) {
            try {

                int seconds = Integer.parseInt(args[0]);
                timeLimitMs = Math.max(1_000, Math.min(60_000, seconds * 1000L));
                System.out.println("[Client] Minuterie: " + (timeLimitMs / 1000) + " secondes");

            } catch (NumberFormatException e) {

                System.err.println("Usage: java Client <seconds>. Using " + (timeLimitMs / 1000) + "s.");
            }
        } else {

            System.out.println("[Client] Time limit: " + (timeLimitMs / 1000) + "s. To match game minuterie run: java Client <seconds> (e.g. java Client 5)");
        }

        try {

            myClient = new Socket("localhost", 8888);

            input = new BufferedInputStream(myClient.getInputStream());
            output = new BufferedOutputStream(myClient.getOutputStream());

            while (true) {

                int c = input.read();

                if (c < 0) {

                    System.out.println("Server closed connection.");
                    break;
                }

                char cmd = (char) c;
                System.out.println(cmd);

                if (cmd == '1') {

                    byte[] aBuffer = new byte[1024];
                    int size = input.available();
                    input.read(aBuffer, 0, size);
                    String s = new String(aBuffer).trim();
                    System.out.println(s);
                    String[] boardValues = s.split(" ");
                    int x = 0, y = 0;

                    for (int i = 0; i < 64 && i < boardValues.length; i++) {

                        board[x][y] = Integer.parseInt(boardValues[i]);
                        x++;
                        if (x == 8) {

                            x = 0;
                            y++;
                        }
                    }

                    if (boardValues.length > 64) {

                        try {

                            int sec = Integer.parseInt(boardValues[64].trim());
                            timeLimitMs = Math.max(1_000, Math.min(60_000, sec * 1000L));
                            System.out.println("[Client] Minuterie from server: " + (timeLimitMs / 1000) + " secondes");

                        } catch (NumberFormatException ignored) { }
                    }
                    gameBoard = new Board(board);
                    ourSide = Mark.rouge;

                    System.out.println("Nouvelle partie! Vous jouez blanc, coup choisi par l'algorithme.");
                    
                    if (gameBoard.isGameOver()) {

                        System.out.println("Partie déjà terminée (condition de fin atteinte).");
                        output.write("0".getBytes(), 0, 1);
                        output.flush();

                    } else {

                        boardBeforeOurMove = new Board(gameBoard);
                        String move = getValidMoveForServer(gameBoard, ourSide, null);
                        lastSentMove = normalizeMove(move);
                        System.out.println("[Client] Sending move: " + move);
                        output.write(move.getBytes(), 0, move.length());
                        output.flush();
                        if (move != null && !move.equals("0")) {

                            gameBoard.makeMove(move);
                        }
                    }
                }

                if (cmd == '2') {

                    System.out.println("Nouvelle partie! Vous jouez noir, attendez le coup des blancs.");
                    byte[] aBuffer = new byte[1024];
                    int size = input.available();
                    input.read(aBuffer, 0, size);
                    String s = new String(aBuffer).trim();
                    System.out.println(s);
                    String[] boardValues = s.split(" ");
                    int x = 0, y = 0;

                    for (int i = 0; i < 64 && i < boardValues.length; i++) {
                        board[x][y] = Integer.parseInt(boardValues[i]);
                        x++;
                        if (x == 8) {
                            x = 0;
                            y++;
                        }
                    }
                    if (boardValues.length > 64) {

                        try {
                            int sec = Integer.parseInt(boardValues[64].trim());
                            timeLimitMs = Math.max(1_000, Math.min(60_000, sec * 1000L));
                            System.out.println("[Client] Minuterie from server: " + (timeLimitMs / 1000) + " secondes");

                        } catch (NumberFormatException ignored) { }
                    }
                    gameBoard = new Board(board);
                    ourSide = Mark.noir;
                }

                if (cmd == '3') {

                    byte[] aBuffer = new byte[16];
                    int size = input.available();
                    System.out.println("size :" + size);
                    input.read(aBuffer, 0, size);

                    String lastMove = new String(aBuffer).trim();
                    System.out.println("Dernier coup : " + lastMove);

                    lastMove = normalizeOpponentMove(lastMove);

                    if (gameBoard != null && lastMove != null && lastMove.length() >= 4) {

                        gameBoard.makeMove(lastMove);
                    }

                    if (gameBoard != null && gameBoard.isGameOver()) {

                        System.out.println("Partie terminée (condition de fin atteinte), pas de coup envoyé.");
                        output.write("0".getBytes(), 0, 1);
                        output.flush();
                    } else {

                        System.out.println("Coup choisi par l'algorithme.");
                        boardBeforeOurMove = new Board(gameBoard);
                        String move = getValidMoveForServer(gameBoard, ourSide, null);
                        lastSentMove = normalizeMove(move);
                        System.out.println("[Client] Sending move: " + move);
                        output.write(move.getBytes(), 0, move.length());
                        output.flush();

                        if (move != null && !move.equals("0")) {

                            gameBoard.makeMove(move);
                        }
                    }
                }

                if (cmd == '4') {

                    System.out.println("Coup invalide, nouveau coup par l'algorithme.");
                    if (gameBoard != null && gameBoard.isGameOver()) {

                        output.write("0".getBytes(), 0, 1);
                        output.flush();
                    } else {

                        if (boardBeforeOurMove != null) {

                            gameBoard = new Board(boardBeforeOurMove);
                        }

                        String move = getValidMoveForServer(gameBoard, ourSide, lastSentMove);
                        lastSentMove = normalizeMove(move);
                        System.out.println("[Client] Sending move (retry): " + move);
                        output.write(move.getBytes(), 0, move.length());
                        output.flush();

                        if (move != null && !move.equals("0")) {

                            boardBeforeOurMove = new Board(gameBoard);
                            gameBoard.makeMove(move);
                        }
                    }
                }

                if (cmd == '5') {

                    byte[] aBuffer = new byte[16];
                    int size = input.available();
                    input.read(aBuffer, 0, size);
                    String s = new String(aBuffer).trim();
                    System.out.println("Partie terminée. Le dernier coup joué est : " + s);
                    output.write("0".getBytes(), 0, 1);
                    output.flush();
                    // Wait for server to close first (so it may not show "connection lost" dialog).
                    try {

                        myClient.setSoTimeout(3000);
                        while (input.read() >= 0) { }

                    } catch (IOException ignored) { }
                    
                    break;
                }
            }
        } catch (IOException e) {

            System.out.println(e);
        } finally {

            try {

                if (output != null) 
                    output.close();

                if (input != null) 
                    input.close();

                if (myClient != null) 
                    myClient.close();

            } catch (IOException e) {

                System.err.println("Error closing connection: " + e.getMessage());
            }

            System.out.println("Connection closed.");
        }
    }

    private static String getMoveFromAI(Board board, Mark sideToMove) {

        if (board == null || sideToMove == null) {

            return "A2A3";
        }

        long limitMs = Math.max(200, timeLimitMs - 200);
        String move = GameAI.getBestMove(board, sideToMove, limitMs);

        if (move == null) {

            var moves = board.generateAllMoves(sideToMove);
            move = moves.isEmpty() ? "A2A3" : moves.get(0);
        }
        return move;
    }

    /** Returns a move to send to the server: valid, formatted, and different from excludeMove if provided (for cmd 4 retry). */
    private static String getValidMoveForServer(Board board, Mark sideToMove, String excludeMove) {

        if (board == null || sideToMove == null) 
            return "0";

        if (board.isGameOver()) 
            return "0";

        java.util.List<String> legal = board.generateAllMoves(sideToMove);

        if (legal.isEmpty()) 
            return "0";

        String chosen = null;
        String excluded = excludeMove != null ? normalizeMove(excludeMove) : null;

        if (excluded != null && legal.size() > 1) {

            for (String m : legal) {

                if (!normalizeMove(m).equals(excluded) && board.isValidMove(m, sideToMove)) {

                    chosen = m;
                    break;
                }
            }
        }
        if (chosen == null) {
            chosen = getMoveFromAI(board, sideToMove);
        }
        if (chosen == null) 
            chosen = legal.get(0);

        if (!board.isValidMove(chosen, sideToMove)) {

            for (String m : legal) {

                if (board.isValidMove(m, sideToMove)) { 
                    chosen = m; break; 
                }
            }
        }
        return formatMoveForServer(chosen);
    }

    private static String normalizeMove(String move) {

        if (move == null) 
            return "";

        return move.replace("-", "").replace(" ", "").trim().toUpperCase();
    }

    /** Normalize opponent move from server (strip brackets, spaces). If SERVER_RANK_1_IS_TOP, flip ranks. */
    private static String normalizeOpponentMove(String move) {

        if (move == null) 
            return null;

        String n = move.replace("[", "").replace("]", "").replace("-", "").replace(" ", "").trim();

        if (n.length() < 4) 
            return move.trim();

        if (SERVER_RANK_1_IS_TOP) {

            int r1 = Character.getNumericValue(n.charAt(1));
            int r2 = Character.getNumericValue(n.charAt(3));
            
            if (r1 >= 1 && r1 <= 8 && r2 >= 1 && r2 <= 8){

                n = "" + n.charAt(0) + (9 - r1) + n.charAt(2) + (9 - r2);
            }
                
        }
        
        return n;
    }

    /** Format move for server: compact "A2A3" (no dash). If SERVER_RANK_1_IS_TOP, flips ranks so server gets correct rows. */
    private static String formatMoveForServer(String move) {

        if (move == null || move.length() < 4) 
            return move == null ? "0" : move;

        String n = move.replace("-", "").replace(" ", "").trim();

        if (n.length() < 4) 
            return move;

        if (SERVER_RANK_1_IS_TOP) {
            int r1 = Character.getNumericValue(n.charAt(1));
            int r2 = Character.getNumericValue(n.charAt(3));
            if (r1 >= 1 && r1 <= 8 && r2 >= 1 && r2 <= 8) {

                n = "" + n.charAt(0) + (9 - r1) + n.charAt(2) + (9 - r2);
            }
        }

        return n;
    }
}

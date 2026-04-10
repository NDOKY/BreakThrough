import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.List;

public class Client {

    private static final boolean SERVER_RANK_1_IS_TOP = false;

    private static long timeLimitMs = 5_000;

    public static void main(String[] args) {

        Socket myClient = null;
        BufferedInputStream input = null;
        BufferedOutputStream output = null;
        int[][] board = new int[8][8];
        Board gameBoard = null;
        Board boardBeforeOurMove = null;
        String lastSentMove = null;
        Mark ourSide = null;
        Mark preferredSide = null;

        if (args.length > 0) {

            for (String argument : args) {

                try {

                    int seconds = Integer.parseInt(argument);
                    timeLimitMs = Math.max(1_000, Math.min(60_000, seconds * 1000L));
                    continue;

                } catch (NumberFormatException ignored) {
                }

                String normalizedArgument = argument.trim().toLowerCase();
                if (normalizedArgument.equals("red") || normalizedArgument.equals("rouge") || normalizedArgument.equals("r")) {
                    preferredSide = Mark.rouge;
                } else if (normalizedArgument.equals("black") || normalizedArgument.equals("noir") || normalizedArgument.equals("b")) {
                    preferredSide = Mark.noir;
                }
            }
        }

        if (preferredSide != null) {
            System.out.println("[Client] Couleur préférée : " + preferredSide);
        }
        System.out.println("[Client] Minuterie : " + (timeLimitMs / 1000) + " s.");

        try {

            myClient = new Socket("localhost", 8888);
            input = new BufferedInputStream(myClient.getInputStream());
            output = new BufferedOutputStream(myClient.getOutputStream());

            while (true) {

                int commandValue = input.read();

                if (commandValue < 0) {
                    System.out.println("Le serveur a fermé la connexion.");
                    break;
                }

                char cmd = (char) commandValue;
                System.out.println(cmd);

                if (cmd == '1') {

                    byte[] aBuffer = new byte[1024];
                    int size = input.available();
                    input.read(aBuffer, 0, size);
                    String boardPayload = new String(aBuffer).trim();
                    System.out.println(boardPayload);
                    String[] boardValues = parseAndFillBoardFromPayload(boardPayload, board);
                    applyServerTimerIfPresent(boardValues);

                    gameBoard = new Board(board);
                    ourSide = Mark.rouge;

                    if (preferredSide != null && preferredSide != ourSide) {
                        System.err.println("[Client] Le serveur vous a assigné les rouges, mais vous préfériez les noirs. Déconnexion.");
                        output.write("0".getBytes(), 0, 1);
                        output.flush();
                        break;
                    }

                    System.out.println("Nouvelle partie! Vous jouez blanc, coup choisi par l'algorithme.");

                    if (gameBoard.isGameOver()) {

                        System.out.println("Partie déjà terminée (condition de fin atteinte).");
                        output.write("0".getBytes(), 0, 1);
                        output.flush();

                    } else {

                        boardBeforeOurMove = new Board(gameBoard);
                        String move = getValidMoveForServer(gameBoard, ourSide, null);
                        lastSentMove = normalizeMove(move);
                        System.out.println("[Client] Envoi du coup : " + move);
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
                    String boardPayload = new String(aBuffer).trim();
                    System.out.println(boardPayload);
                    String[] boardValues = parseAndFillBoardFromPayload(boardPayload, board);
                    applyServerTimerIfPresent(boardValues);

                    gameBoard = new Board(board);
                    ourSide = Mark.noir;

                    if (preferredSide != null && preferredSide != ourSide) {
                        System.err.println("[Client] Le serveur vous a assigné les noirs, mais vous préfériez les rouges. Déconnexion.");
                        output.write("0".getBytes(), 0, 1);
                        output.flush();
                        break;
                    }
                }

                if (cmd == '3') {

                    byte[] aBuffer = new byte[16];
                    int size = input.available();
                    input.read(aBuffer, 0, size);

                    String lastMove = new String(aBuffer).trim();
                    System.out.println("Dernier coup : " + lastMove);

                    lastMove = normalizeOpponentMove(lastMove);

                    if (gameBoard != null && ourSide != null && lastMove != null && lastMove.length() >= 4) {

                        Mark opponentSide = ourSide == Mark.rouge ? Mark.noir : Mark.rouge;
                        if (!isInvalidMovePlaceholder(lastMove)) {
                            if (gameBoard.isValidMove(lastMove, opponentSide)) {
                                gameBoard.makeMove(lastMove);
                            } else {
                                System.err.println("[Client] Coup adverse invalide reçu et ignoré : " + lastMove);
                            }
                        }
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
                        System.out.println("[Client] Envoi du coup : " + move);
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
                        System.out.println("[Client] Envoi du coup (nouvel essai) : " + move);
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
                    String finalMoveMessage = new String(aBuffer).trim();
                    System.out.println("Partie terminée. Le dernier coup joué est : " + finalMoveMessage);
                    output.write("0".getBytes(), 0, 1);
                    output.flush();

                    try {
                        myClient.setSoTimeout(3000);
                        while (input.read() >= 0) { }
                    } catch (IOException ignored) {
                    }

                    break;
                }
            }

        } catch (IOException ioException) {
            System.out.println("Erreur d'E/S : " + ioException);

        } finally {

            try {

                if (output != null) {
                    output.close();
                }

                if (input != null) {
                    input.close();
                }

                if (myClient != null) {
                    myClient.close();
                }

            } catch (IOException ioException) {
                System.err.println("Erreur à la fermeture de la connexion : " + ioException.getMessage());
            }

            System.out.println("Connexion fermée.");
        }
    }

    private static String[] parseAndFillBoardFromPayload(String boardPayload, int[][] board) {

        String[] boardValues = boardPayload.split(" ");
        int columnIndex = 0;
        int rowIndex = 0;

        for (int valueIndex = 0; valueIndex < 64 && valueIndex < boardValues.length; valueIndex++) {

            board[columnIndex][rowIndex] = Integer.parseInt(boardValues[valueIndex]);
            columnIndex++;
            if (columnIndex == 8) {
                columnIndex = 0;
                rowIndex++;
            }
        }

        return boardValues;
    }

    private static void applyServerTimerIfPresent(String[] boardValues) {

        if (boardValues.length <= 64) {
            return;
        }

        try {

            int sec = Integer.parseInt(boardValues[64].trim());
            timeLimitMs = Math.max(1_000, Math.min(60_000, sec * 1000L));
            System.out.println("[Client] Minuterie du serveur : " + (timeLimitMs / 1000) + " secondes");

        } catch (NumberFormatException ignored) {
        }
    }

    private static String getMoveFromAI(Board board, Mark sideToMove) {

        if (board == null || sideToMove == null) {
            return "A2A3";
        }

        long limitMs = Math.max(200, timeLimitMs - 200);
        String move = GameAI.getBestMove(board, sideToMove, limitMs);

        if (move == null) {
            List<String> moves = board.generateAllMoves(sideToMove);
            move = moves.isEmpty() ? "A2A3" : moves.get(0);
        }

        return move;
    }

    private static String getValidMoveForServer(Board board, Mark sideToMove, String excludeMove) {

        if (board == null || sideToMove == null) {
            return "0";
        }

        if (board.isGameOver()) {
            return "0";
        }

        List<String> legal = board.generateAllMoves(sideToMove);

        if (legal.isEmpty()) {
            return "0";
        }

        String chosen = null;
        String excluded = excludeMove != null ? normalizeMove(excludeMove) : null;

        if (excluded != null && legal.size() > 1) {

            for (String legalMove : legal) {

                if (!normalizeMove(legalMove).equals(excluded) && board.isValidMove(legalMove, sideToMove)) {
                    chosen = legalMove;
                    break;
                }
            }
        }

        if (chosen == null) {
            chosen = getMoveFromAI(board, sideToMove);
        }

        return formatMoveForServer(chosen);
    }

    private static String normalizeMove(String move) {

        if (move == null) {
            return "";
        }

        return move.replace("-", "").replace(" ", "").trim().toUpperCase();
    }

    private static boolean isInvalidMovePlaceholder(String move) {

        String normalizedMove = normalizeMove(move);
        return normalizedMove.length() >= 4
                && normalizedMove.charAt(0) == normalizedMove.charAt(2)
                && normalizedMove.charAt(1) == normalizedMove.charAt(3);
    }

    private static String normalizeOpponentMove(String move) {

        if (move == null) {
            return null;
        }

        String normalizedMove = move.replace("[", "").replace("]", "").replace("-", "").replace(" ", "").trim();

        if (normalizedMove.length() < 4) {
            return move.trim();
        }

        if (SERVER_RANK_1_IS_TOP) {

            int fromRank = Character.getNumericValue(normalizedMove.charAt(1));
            int toRank = Character.getNumericValue(normalizedMove.charAt(3));

            if (fromRank >= 1 && fromRank <= 8 && toRank >= 1 && toRank <= 8) {
                normalizedMove = "" + normalizedMove.charAt(0) + (9 - fromRank) + normalizedMove.charAt(2) + (9 - toRank);
            }
        }

        return normalizedMove;
    }

    private static String formatMoveForServer(String move) {

        if (move == null || move.length() < 4) {
            return move == null ? "0" : move;
        }

        String normalizedMove = move.replace("-", "").replace(" ", "").trim();

        if (normalizedMove.length() < 4) {
            return move;
        }

        if (SERVER_RANK_1_IS_TOP) {

            int fromRank = Character.getNumericValue(normalizedMove.charAt(1));
            int toRank = Character.getNumericValue(normalizedMove.charAt(3));
            if (fromRank >= 1 && fromRank <= 8 && toRank >= 1 && toRank <= 8) {
                normalizedMove = "" + normalizedMove.charAt(0) + (9 - fromRank) + normalizedMove.charAt(2) + (9 - toRank);
            }
        }

        return normalizedMove;
    }
}

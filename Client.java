import java.awt.GraphicsEnvironment;
import java.awt.GridLayout;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.List;

import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;

public class Client {

    private static final int DEFAULT_SERVER_PORT = 8888;

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

        LaunchConfig launch = parseLaunchArguments(args);

        if (args.length == 0 && !GraphicsEnvironment.isHeadless()) {

            promptLaunchOptionsFromDialog(launch);
        }

        Mark preferredSide = launch.preferredSide;

        System.out.println("[Client] Couleur préférée : " + preferredSide);
        System.out.println("[Client] Minuterie : " + (timeLimitMs / 1000) + " s.");
        System.out.println("[Client] Serveur : " + launch.serverHost + ":" + launch.serverPort);

        try {

            myClient = new Socket(launch.serverHost, launch.serverPort);
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

                    if (preferredSide != ourSide) {

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

                    if (preferredSide != ourSide) {

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

                        while (input.read() >= 0) { 

                        }

                    } catch (IOException ignored) {

                    }

                    break;
                }
            }

        } catch (IOException ioException) {

            System.out.println("Erreur de Input/Output : " + ioException);

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

    private static final class LaunchConfig {

        String serverHost = "localhost";
        int serverPort = DEFAULT_SERVER_PORT;
        Mark preferredSide = Mark.rouge;
    }

    private static LaunchConfig parseLaunchArguments(String[] args) {

        LaunchConfig config = new LaunchConfig();
        int index = 0;

        while (index < args.length) {

            String token = args[index].trim();
            String lower = token.toLowerCase();

            if (lower.equals("--help") || lower.equals("-?")) {

                printUsage();
                System.exit(0);
            }

            if (lower.equals("--nogui") || lower.equals("--no-gui")) {

                index++;
                continue;
            }

            if (lower.equals("--host") || lower.equals("--hote") || lower.equals("-H")) {

                if (index + 1 >= args.length) {
                    
                    System.err.println("[Client] --host nécessite une adresse.");
                    printUsage();
                    System.exit(1);
                }

                config.serverHost = args[index + 1].trim();
                index += 2;
                continue;
            }

            if (lower.equals("--port") || lower.equals("-p")) {

                if (index + 1 >= args.length) {

                    System.err.println("[Client] --port nécessite un numéro.");
                    printUsage();
                    System.exit(1);
                }

                try {

                    config.serverPort = parsePort(args[index + 1]);

                } catch (IllegalArgumentException ex) {

                    System.err.println("[Client] " + ex.getMessage());
                    System.exit(1);
                }

                index += 2;
                continue;
            }

            try {

                int seconds = Integer.parseInt(token);
                timeLimitMs = Math.max(1_000, Math.min(60_000, seconds * 1000L));
                index++;
                continue;

            } catch (NumberFormatException ignored) {

            }

            if (lower.equals("red") || lower.equals("rouge") || lower.equals("r")) {

                config.preferredSide = Mark.rouge;

            } else if (lower.equals("black") || lower.equals("noir") || lower.equals("b")) {
                
                config.preferredSide = Mark.noir;

            } else {

                System.err.println("[Client] Argument non reconnu : " + args[index]);
                printUsage();
                System.exit(1);
            }

            index++;
        }

        return config;
    }

    private static int parsePort(String text) {

        int port;

        try {
            port = Integer.parseInt(text.trim());

        } catch (NumberFormatException ex) {

            throw new IllegalArgumentException("Port non numérique : " + text);
        }

        if (port < 1 || port > 65_535) {

            throw new IllegalArgumentException("Port invalide (1–65535) : " + port);
        }

        return port;
    }

    private static void printUsage() {

        System.err.println("Usage : java -jar BreakThrough.jar [options] [secondes] [couleur]");
        System.err.println("  --host ADR, --hote ADR, -H ADR   adresse du serveur (défaut : localhost)");
        System.err.println("  --port N, -p N                   port TCP (défaut : " + DEFAULT_SERVER_PORT + ")");
        System.err.println("  --nogui, --no-gui                pas de fenêtre (ligne de commande / mode sans affichage)");
        System.err.println("  (sans arguments, si affichage OK)  fenêtre hôte, port, secondes, couleur (ex. double-clic sur le .jar)");
        System.err.println("  secondes                         minuterie par coup, 1–60");
        System.err.println("  rouge|noir|r|b|red|black         couleur préférée (défaut : rouge)");
        System.err.println("Ex. : java -jar BreakThrough.jar --host 192.168.0.12 -p 8888 5");
        System.err.println("      java -jar BreakThrough.jar --host 192.168.0.12 -p 8888 5 noir");
        System.err.println("      java -jar BreakThrough.jar --nogui");
    }

    private static void promptLaunchOptionsFromDialog(LaunchConfig config) {

        int secondsShown = (int) (timeLimitMs / 1_000L);
        secondsShown = Math.max(1, Math.min(60, secondsShown));

        JPanel panel = new JPanel(new GridLayout(0, 1, 4, 4));
        JTextField hostField = new JTextField(config.serverHost, 24);
        JTextField portField = new JTextField(String.valueOf(config.serverPort), 8);
        JTextField secondsField = new JTextField(String.valueOf(secondsShown), 4);
        JComboBox<String> colorCombo = new JComboBox<>(new String[] {"Rouge", "Noir"});
        colorCombo.setSelectedItem(config.preferredSide == Mark.noir ? "Noir" : "Rouge");

        panel.add(new JLabel("Adresse du serveur (IP ou nom d'hôte) :"));
        panel.add(hostField);
        panel.add(new JLabel("Port :"));
        panel.add(portField);
        panel.add(new JLabel("Minuterie par coup (secondes, 1–60) :"));
        panel.add(secondsField);
        panel.add(new JLabel("Couleur préférée :"));
        panel.add(colorCombo);

        int result = JOptionPane.showConfirmDialog(
                null,
                panel,
                "Connexion et options BreakThrough",
                JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.QUESTION_MESSAGE);

        if (result != JOptionPane.OK_OPTION) {

            System.out.println("[Client] Démarrage annulé.");
            System.exit(0);
        }

        String host = hostField.getText().trim();

        if (!host.isEmpty()) {

            config.serverHost = host;
        }

        try {

            config.serverPort = parsePort(portField.getText());

        } catch (IllegalArgumentException ex) {

            JOptionPane.showMessageDialog(null, ex.getMessage(), "Erreur", JOptionPane.ERROR_MESSAGE);
            System.exit(1);

        }

        try {

            int seconds = Integer.parseInt(secondsField.getText().trim());
            timeLimitMs = Math.max(1_000, Math.min(60_000, seconds * 1000L));

        } catch (NumberFormatException ex) {

            JOptionPane.showMessageDialog(null, "Secondes invalides (entier attendu).", "Erreur", JOptionPane.ERROR_MESSAGE);
            System.exit(1);

        }

        Object selected = colorCombo.getSelectedItem();

        if ("Noir".equals(selected)) {

            config.preferredSide = Mark.noir;
            
        } else {

            config.preferredSide = Mark.rouge;
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

        return normalizedMove;
    }
}

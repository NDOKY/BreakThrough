/**
 * Represents the type of piece (or empty) on a Breakthrough board.
 * Matches server protocol: 0 = vide, 2 = noir, 4 = rouge.
 */
enum Mark {

    noir(2),
    rouge(4),
    vide(0);

    private final int serverCode;

    Mark(int serverCode) {
        this.serverCode = serverCode;
    }

    public int toServerCode() {
        return serverCode;
    }

    public static Mark fromServerCode(int serverCode) {
        switch (serverCode) {
            case 2: return noir;
            case 4: return rouge;
            default: return vide;
        }
    }
}

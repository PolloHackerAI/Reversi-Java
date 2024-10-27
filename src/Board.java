public class Board {
    private char[][] grid;

    // Costruttore: inizializza la scacchiera
    public Board() {
        grid = new char[8][8];
        for (int i = 0; i < 8; i++) {
            for (int j = 0; j < 8; j++) {
                grid[i][j] = '-';
            }
        }
        // Posiziona i pezzi iniziali
        grid[3][3] = 'W';
        grid[3][4] = 'B';
        grid[4][3] = 'B';
        grid[4][4] = 'W';
    }

    // Restituisce il pezzo in una data posizione
    public char getPiece(int row, int col) {
        return grid[row][col];
    }

    // Verifica se il giocatore ha mosse valide e conta i pezzi
    public String checkValidMovesAndCount() {
        boolean hasValidMoveBlack = false;
        boolean hasValidMoveWhite = false;
        int blackCount = 0;
        int whiteCount = 0;

        for (int i = 0; i < 8; i++) {
            for (int j = 0; j < 8; j++) {
                if (grid[i][j] == 'B') {
                    blackCount++;
                } else if (grid[i][j] == 'W') {
                    whiteCount++;
                }
                if (!hasValidMoveBlack && isValidMove(i, j, 'B')) {
                    hasValidMoveBlack = true;
                }
                if (!hasValidMoveWhite && isValidMove(i, j, 'W')) {
                    hasValidMoveWhite = true;
                }
            }
        }

        String result = "Pezzi neri: " + blackCount + ", Pezzi bianchi: " + whiteCount + "\n";
        if (!hasValidMoveBlack && !hasValidMoveWhite) {
            if (blackCount > whiteCount) {
                result += "Il nero ha vinto!";
            } else if (whiteCount > blackCount) {
                result += "Il bianco ha vinto!";
            } else {
                result += "Pareggio!";
            }
        } else {
            result += "La partita continua.";
        }

        return result;
    }

    // Verifica se una mossa è valida
    public boolean isValidMove(int row, int col, char color) {
        if (grid[row][col] != '-') {
            return false;
        }

        char opponent = (color == 'B') ? 'W' : 'B';

        for (int dr = -1; dr <= 1; dr++) {
            for (int dc = -1; dc <= 1; dc++) {
                if (dr == 0 && dc == 0) continue;

                int r = row + dr;
                int c = col + dc;
                boolean foundOpponent = false;

                while (r >= 0 && r < 8 && c >= 0 && c < 8) {
                    if (grid[r][c] == opponent) {
                        foundOpponent = true;
                    } else if (grid[r][c] == color && foundOpponent) {
                        return true;
                    } else {
                        break;
                    }
                    r += dr;
                    c += dc;
                }
            }
        }

        return false;
    }

    // Esegue una mossa
    public void makeMove(int row, int col, char color) {
        grid[row][col] = color;
        char opponent = (color == 'B') ? 'W' : 'B';

        for (int dr = -1; dr <= 1; dr++) {
            for (int dc = -1; dc <= 1; dc++) {
                if (dr == 0 && dc == 0) continue;

                int r = row + dr;
                int c = col + dc;
                boolean foundOpponent = false;
                int flipCount = 0;

                while (r >= 0 && r < 8 && c >= 0 && c < 8) {
                    if (grid[r][c] == opponent) {
                        foundOpponent = true;
                        flipCount++;
                    } else if (grid[r][c] == color && foundOpponent) {
                        for (int i = 1; i <= flipCount; i++) {
                            grid[row + i * dr][col + i * dc] = color;
                        }
                        break;
                    } else {
                        break;
                    }
                    r += dr;
                    c += dc;
                }
            }
        }
    }

    // Verifica se il gioco è finito
    public boolean isGameOver() {
        for (int i = 0; i < 8; i++) {
            for (int j = 0; j < 8; j++) {
                if (isValidMove(i, j, 'B') || isValidMove(i, j, 'W')) {
                    return false;
                }
            }
        }
        return true;
    }

    public int countPieces(char color) {
        int count = 0;
        for (int row = 0; row < grid.length; row++) {
            for (int col = 0; col < grid[row].length; col++) {
                if (grid[row][col] == color) {
                    count++;
                }
            }
        }
        return count;
    }
}
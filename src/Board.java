import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

class Board {
    private char[][] grid;

    public Board() {
        grid = new char[8][8];
        for (int i = 0; i < 8; i++)
            Arrays.fill(grid[i], '-');
        grid[3][3] = 'W';
        grid[3][4] = 'B';
        grid[4][3] = 'B';
        grid[4][4] = 'W';
    }

    public char getPiece(int row, int col) {
        return grid[row][col];
    }

    public boolean isValidMove(int row, int col, char color) {
        if (grid[row][col] != '-') return false;

        boolean foundOpponent = false;
        char opponent = (color == 'B') ? 'W' : 'B';
        for (int[] dir : new int[][]{{1, 0}, {0, 1}, {1, 1}, {-1, 0}, {0, -1}, {-1, -1}, {1, -1}, {-1, 1}}) {
            if (checkDirection(row, col, dir[0], dir[1], color, opponent)) {
                foundOpponent = true;
            }
        }
        return foundOpponent;
    }

    private boolean checkDirection(int row, int col, int dr, int dc, char color, char opponent) {
        int r = row + dr, c = col + dc;
        boolean foundOpponent = false;
        while (r >= 0 && r < 8 && c >= 0 && c < 8 && grid[r][c] == opponent) {
            foundOpponent = true;
            r += dr;
            c += dc;
        }
        return foundOpponent && r >= 0 && r < 8 && c >= 0 && c < 8 && grid[r][c] == color;
    }

    public void makeMove(int row, int col, char color) {
        grid[row][col] = color;
        flipPieces(row, col, color);
    }

    private void flipPieces(int row, int col, char color) {
        char opponent = (color == 'B') ? 'W' : 'B';
        for (int[] dir : new int[][]{{1, 0}, {0, 1}, {1, 1}, {-1, 0}, {0, -1}, {-1, -1}, {1, -1}, {-1, 1}}) {
            if (checkDirection(row, col, dir[0], dir[1], color, opponent)) {
                flipDirection(row, col, dir[0], dir[1], color);
            }
        }
    }

    private void flipDirection(int row, int col, int dr, int dc, char color) {
        int r = row + dr, c = col + dc;
        while (grid[r][c] != color) {
            grid[r][c] = color;
            r += dr;
            c += dc;
        }
    }

    public boolean hasValidMoves(char color) {
        for (int i = 0; i < 8; i++) {
            for (int j = 0; j < 8; j++) {
                if (isValidMove(i, j, color)) {
                    return true;
                }
            }
        }
        return false;
    }

    public int countPieces(char color) {
        int count = 0;
        for (char[] row : grid) {
            for (char piece : row) {
                if (piece == color) count++;
            }
        }
        return count;
    }

    public List<int[]> getValidMoves(char color) {
        List<int[]> moves = new ArrayList<>();
        for (int i = 0; i < 8; i++) {
            for (int j = 0; j < 8; j++) {
                if (isValidMove(i, j, color)) {
                    moves.add(new int[]{i, j});
                }
            }
        }
        return moves;
    }
}

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.net.*;
import java.util.*;

public class ReversiGame extends JFrame {
    private JPanel[][] cells;
    private Board board;
    private JButton surrenderButton;
    private JLabel statusLabel;
    private boolean isServer, myTurn, playingVsComputer = false;
    private char myColor;
    private NetworkManager networkManager;

    public ReversiGame() {
        setTitle("Gioco Reversi");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(450, 550);
        setLayout(new BorderLayout());
        setLocationRelativeTo(null);
        showStartPanel();
        setVisible(true);
    }

    private void showStartPanel() {
        var startPanel = new JPanel(new GridLayout(3, 1, 5, 5));
        Arrays.asList("Ospita Partita", "Unisciti a Partita", "Gioca contro Computer").forEach(label -> {
            var button = new JButton(label);
            button.addActionListener(e -> startGame("Gioca contro Computer".equals(label), label.equals("Ospita Partita")));
            startPanel.add(button);
        });
        updateContent(startPanel);
    }

    private void startGame(boolean vsComputer, boolean asServer) {
        playingVsComputer = vsComputer;
        isServer = asServer;
        if (!vsComputer) setupNetwork(asServer);
        myColor = asServer || vsComputer ? 'B' : 'W';
        myTurn = myColor == 'B';
        setupBoard();
    }

    private void setupNetwork(boolean asServer) {
        networkManager = new NetworkManager();
        try {
            if (asServer) {
                int port = new Random().nextInt(16384) + 49152;
                String ipAddress = InetAddress.getLocalHost().getHostAddress();
                JOptionPane.showMessageDialog(this, "In attesa di un avversario... IP: " + ipAddress + " Porta: " + port);
                networkManager.setupServer(port);
                new Server().start();
            } else {
                String ipAddress = JOptionPane.showInputDialog("Indirizzo IP del server:");
                int port = Integer.parseInt(JOptionPane.showInputDialog("Numero di porta:", "5000"));
                networkManager.setupClient(ipAddress, port);
                new Client().start();
            }
        } catch (IOException e) {
            showError(e);
            showStartPanel();
        }
    }

    private void setupBoard() {
        board = new Board();
        cells = new JPanel[8][8];
        var boardPanel = new JPanel(new GridLayout(8, 8, 2, 2));
        boardPanel.setBackground(Color.BLACK);
        for (int i = 0; i < 8; i++) {
            for (int j = 0; j < 8; j++) {
                cells[i][j] = new JPanel();
                cells[i][j].setBackground(new Color(0, 100, 0));
                cells[i][j].addMouseListener(new MoveListener(i, j));
                boardPanel.add(cells[i][j]);
            }
        }
        var controlPanel = new JPanel(new BorderLayout());
        surrenderButton = new JButton(playingVsComputer ? "Arrenditi" : "Abbandona");
        surrenderButton.addActionListener(e -> handleSurrender());
        statusLabel = new JLabel("Inizia la partita!", JLabel.CENTER);
        controlPanel.add(surrenderButton, BorderLayout.NORTH);
        controlPanel.add(statusLabel, BorderLayout.SOUTH);
        addComponents(boardPanel, controlPanel);
        updateBoard();
    }

    private void updateBoard() {
        SwingUtilities.invokeLater(() -> {
            for (int i = 0; i < 8; i++) {
                for (int j = 0; j < 8; j++) {
                    cells[i][j].removeAll();
                    char piece = board.getPiece(i, j);
                    if (piece != '-') {
                        var piecePanel = new JPanel();
                        piecePanel.setPreferredSize(new Dimension(40, 40));
                        piecePanel.setBackground(piece == 'B' ? Color.BLACK : Color.WHITE);
                        piecePanel.setBorder(BorderFactory.createLineBorder(Color.BLACK, 1));
                        cells[i][j].add(piecePanel);
                    }
                }
            }
            updateStatusLabel();
        });
    }

    private void makeMove(int row, int col) {
        if (myTurn && board.isValidMove(row, col, myColor)) {
            board.makeMove(row, col, myColor);
            updateBoard();
            myTurn = false;
            if (playingVsComputer) {
                new javax.swing.Timer(500, e -> makeComputerMove()).start();
            } else {
                networkManager.sendMove(row, col);
            }
            checkGameOver();
        }
    }

    private void makeComputerMove() {
        var validMoves = board.getValidMoves('W');
        if (!validMoves.isEmpty()) {
            var move = validMoves.get(new Random().nextInt(validMoves.size()));
            board.makeMove(move[0], move[1], 'W');
            updateBoard();
            myTurn = true;
            checkGameOver();
        } else {
            JOptionPane.showMessageDialog(this, "Il computer non ha mosse disponibili. È il tuo turno.");
        }
    }

    private void handleSurrender() {
        if (playingVsComputer) {
            askForNewGame();
        } else if (networkManager != null) {
            networkManager.sendSurrender();
            JOptionPane.showMessageDialog(this, "Ti sei arreso!");
            showStartPanel();
        }
    }

    private void checkGameOver() {
        if (!board.hasValidMoves('B') && !board.hasValidMoves('W')) {
            var blackCount = board.countPieces('B');
            var whiteCount = board.countPieces('W');
            String result = blackCount > whiteCount ? "Nero vince!" : (blackCount < whiteCount ? "Bianco vince!" : "Pareggio!");
            JOptionPane.showMessageDialog(this, result);
            askForNewGame();
        }
    }

    private void askForNewGame() {
        if (JOptionPane.showConfirmDialog(this, "Vuoi giocare di nuovo?", "Partita Terminata", JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) {
            if (playingVsComputer) {
                startGame(true, false);
            } else if (isServer) {
                setupNetwork(true);
            } else {
                setupNetwork(false);
            }
        } else {
            showStartPanel();
        }
    }

    private void updateStatusLabel() {
        int blackCount = board.countPieces('B');
        int whiteCount = board.countPieces('W');
        statusLabel.setText("Nero: " + blackCount + " | Bianco: " + whiteCount);
    }

    private class MoveListener extends MouseAdapter {
        private final int row, col;

        public MoveListener(int row, int col) {
            this.row = row;
            this.col = col;
        }

        public void mouseClicked(MouseEvent e) {
            makeMove(row, col);
        }
    }

    private class Server extends Thread {
        public void run() {
            try {
                networkManager.waitForClient();
                setupStreams();
            } catch (IOException e) {
                showError(e);
            }
        }
    }

    private class Client extends Thread {
        public void run() {
            try {
                setupStreams();
            } catch (IOException e) {
                showError(e);
                showStartPanel();
            }
        }
    }

    private void setupStreams() {
        new Thread(() -> {
            try {
                String inputLine;
                while ((inputLine = networkManager.receiveMessage()) != null) {
                    processInput(inputLine);
                }
            } catch (IOException e) {
                showError(e);
            }
        }).start();
    }

    private void processInput(String input) {
        if (input.startsWith("MOVE")) {
            String[] parts = input.split(" ");
            board.makeMove(Integer.parseInt(parts[1]), Integer.parseInt(parts[2]), myColor == 'B' ? 'W' : 'B');
            updateBoard();
            myTurn = true;
            checkGameOver();
        } else if (input.equals("SURRENDER")) {
            JOptionPane.showMessageDialog(this, "L'avversario si è arreso!");
        }
    }

    private void showError(Exception e) {
        e.printStackTrace();
        JOptionPane.showMessageDialog(this, "Errore: " + e.getMessage());
    }

    private void updateContent(JComponent comp) {
        getContentPane().removeAll();
        add(comp, BorderLayout.CENTER);
        revalidate();
        repaint();
    }

    private void addComponents(JComponent board, JComponent control) {
        updateContent(new JPanel(new BorderLayout()) {{
            add(board, BorderLayout.CENTER);
            add(control, BorderLayout.SOUTH);
        }});
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(ReversiGame::new);
    }
}

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
    private String ipAddress;
    private int port;
    private Socket socket;
    private PrintWriter out;
    private BufferedReader in;
    private ServerSocket serverSocket;

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
        try {
            if (asServer) {
                port = new Random().nextInt(16384) + 49152;
                ipAddress = InetAddress.getLocalHost().getHostAddress();
                serverSocket = new ServerSocket(port);
                JOptionPane.showMessageDialog(this, "In attesa di un avversario... IP: " + ipAddress + " Porta: " + port);
                new Server().start();
            } else {
                ipAddress = JOptionPane.showInputDialog("Indirizzo IP del server:");
                port = Integer.parseInt(JOptionPane.showInputDialog("Numero di porta:", "5000"));
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
        for (int i = 0; i < 8; i++) for (int j = 0; j < 8; j++) {
            cells[i][j] = new JPanel();
            cells[i][j].setBackground(new Color(0, 100, 0));
            cells[i][j].addMouseListener(new MoveListener(i, j));
            boardPanel.add(cells[i][j]);
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
            for (int i = 0; i < 8; i++) for (int j = 0; j < 8; j++) {
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
            updateStatusLabel();
        });
    }

    private void makeMove(int row, int col) {
        if (myTurn && board.isValidMove(row, col, myColor)) {
            board.makeMove(row, col, myColor);
            updateBoard();
            myTurn = false;
            if (playingVsComputer) new javax.swing.Timer(500, e -> makeComputerMove()).start();
            else sendMove(row, col);
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
        } else JOptionPane.showMessageDialog(this, "Il computer non ha mosse disponibili. È il tuo turno.");
    }

    private void handleSurrender() {
        if (playingVsComputer) askForNewGame();
        else if (out != null) {
            out.println("SURRENDER");
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

    private void sendMove(int row, int col) {
        if (out != null) out.println("MOVE " + row + " " + col);
    }

    private void askForNewGame() {
        if (JOptionPane.showConfirmDialog(this, "Vuoi giocare di nuovo?", "Partita Terminata", JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) {
            if (playingVsComputer) startGame(true, false);
            else if (isServer) setupNetwork(true);
            else setupNetwork(false);
        } else showStartPanel();
    }

    private void updateStatusLabel() {
        int blackCount = board.countPieces('B');
        int whiteCount = board.countPieces('W');
        statusLabel.setText("Nero: " + blackCount + " | Bianco: " + whiteCount);
    }

    private class MoveListener extends MouseAdapter {
        private final int row, col;
        public MoveListener(int row, int col) { this.row = row; this.col = col; }
        public void mouseClicked(MouseEvent e) { makeMove(row, col); }
    }

    private class Server extends Thread {
        public void run() {
            try {
                socket = serverSocket.accept();
                setupStreams();
            } catch (IOException e) {
                showError(e);
            }
        }
    }

    private class Client extends Thread {
        public void run() {
            try {
                socket = new Socket(ipAddress, port);
                setupStreams();
            } catch (IOException e) {
                showError(e);
                showStartPanel();
            }
        }
    }

    private void setupStreams() {
        try {
            out = new PrintWriter(socket.getOutputStream(), true);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            processInput();
        } catch (IOException e) {
            showError(e);
        }
    }

    private void processInput() {
        new Thread(() -> {
            try {
                String inputLine;
                while ((inputLine = in.readLine()) != null) {
                    if (inputLine.startsWith("MOVE")) {
                        String[] parts = inputLine.split(" ");
                        board.makeMove(Integer.parseInt(parts[1]), Integer.parseInt(parts[2]), myColor == 'B' ? 'W' : 'B');
                        updateBoard();
                        myTurn = true;
                        checkGameOver();
                    } else if (inputLine.equals("SURRENDER")) {
                        JOptionPane.showMessageDialog(this, "L'avversario si è arreso!");
                    }
                }
            } catch (IOException e) {
                showError(e);
            }
        }).start();
    }

    private void showError(Exception e) {
        JOptionPane.showMessageDialog(this, "Errore: " + e.getMessage(), "Errore", JOptionPane.ERROR_MESSAGE);
        e.printStackTrace();
    }

    private void updateContent(JComponent comp) {
        getContentPane().removeAll();
        add(comp, BorderLayout.CENTER);
        revalidate();
        repaint();
    }

    private void addComponents(JComponent board, JComponent control) {
        var panel = new JPanel(new BorderLayout());
        panel.add(board, BorderLayout.CENTER);
        panel.add(control, BorderLayout.SOUTH);
        updateContent(panel);
    }

    public static void main(String[] args) { SwingUtilities.invokeLater(ReversiGame::new); }
}

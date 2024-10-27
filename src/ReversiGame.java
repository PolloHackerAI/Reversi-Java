import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.net.*;
import java.util.Random;
import java.util.ArrayList;

public class ReversiGame extends JFrame {
    private JPanel[][] cells; // Griglia di celle per la tavola di gioco
    private Board board; // Oggetto che rappresenta lo stato della tavola di gioco
    private JButton surrenderButton; // Pulsante per arrendersi o abbandonare
    private JLabel statusLabel; // Etichetta per mostrare lo stato del gioco
    private boolean isServer; // Indica se questa istanza è il server
    private Socket socket; // Socket per la comunicazione di rete
    private PrintWriter out; // Writer per inviare messaggi
    private BufferedReader in; // Reader per ricevere messaggi
    private boolean myTurn; // Indica se è il turno del giocatore corrente
    private char myColor; // Colore del giocatore corrente ('B' per nero, 'W' per bianco)
    private int port; // Porta per la connessione di rete
    private String ipAddress; // Indirizzo IP per la connessione di rete
    private ServerSocket serverSocket; // ServerSocket per accettare connessioni in entrata
    private boolean playingVsComputer = false; // Indica se si sta giocando contro il computer

    // Costruttore della classe ReversiGame
    public ReversiGame() {
        setTitle("Gioco Reversi");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(450, 550);
        setLayout(new BorderLayout());
        showStartPanel();
        setLocationRelativeTo(null);
        setVisible(true);
    }

    // Mostra il pannello iniziale con le opzioni di gioco
    private void showStartPanel() {
        getContentPane().removeAll();
        JPanel startPanel = new JPanel(new GridLayout(3, 1, 5, 5));
        JButton hostButton = new JButton("Ospita Partita");
        JButton joinButton = new JButton("Unisciti a Partita");
        JButton computerButton = new JButton("Gioca contro Computer");

        hostButton.addActionListener(e -> startGame(true));
        joinButton.addActionListener(e -> startGame(false));
        computerButton.addActionListener(e -> startGameVsComputer());

        startPanel.add(hostButton);
        startPanel.add(joinButton);
        startPanel.add(computerButton);
        add(startPanel, BorderLayout.CENTER);
        revalidate();
        repaint();
    }

    // Avvia il gioco come server o client
    private void startGame(boolean asServer) {
        isServer = asServer;
        playingVsComputer = false;
        getContentPane().removeAll();

        if (isServer) {
            port = findAvailablePort();
            ipAddress = getLocalIPAddress();
            myColor = 'B';
            myTurn = true;
            JOptionPane.showMessageDialog(this,
                    "In attesa di un avversario...\n" +
                            "IP: " + ipAddress + "\n" +
                            "Porta: " + port);
            new Server().start();
        } else {
            ipAddress = JOptionPane.showInputDialog("Inserisci l'indirizzo IP del server:");
            port = Integer.parseInt(JOptionPane.showInputDialog("Inserisci il numero di porta:", "5000"));
            myColor = 'W';
            myTurn = false;
            new Client().start();
        }

        setupBoard();
    }

    // Ottiene l'indirizzo IP locale
    private String getLocalIPAddress() {
        try {
            return InetAddress.getLocalHost().getHostAddress();
        } catch (UnknownHostException e) {
            e.printStackTrace();
            return "127.0.0.1";
        }
    }

    // Avvia il gioco contro il computer
    private void startGameVsComputer() {
        playingVsComputer = true;
        getContentPane().removeAll();
        setupBoard();
        myColor = 'B';
        myTurn = true;
    }

    // Configura la tavola di gioco
    private void setupBoard() {
        board = new Board();
        cells = new JPanel[8][8];
        JPanel boardPanel = new JPanel(new GridLayout(8, 8, 2, 2));
        boardPanel.setBackground(Color.BLACK);

        for (int i = 0; i < 8; i++) {
            for (int j = 0; j < 8; j++) {
                cells[i][j] = new JPanel();
                cells[i][j].setBackground(new Color(0, 100, 0));
                cells[i][j].addMouseListener(new MoveListener(i, j));
                boardPanel.add(cells[i][j]);
            }
        }

        add(boardPanel, BorderLayout.CENTER);

        JPanel controlPanel = new JPanel(new BorderLayout());
        surrenderButton = new JButton(playingVsComputer ? "Arrenditi" : "Abbandona");
        surrenderButton.addActionListener(e -> handleSurrender());
        controlPanel.add(surrenderButton, BorderLayout.NORTH);

        statusLabel = new JLabel("Inizia la partita!");
        statusLabel.setHorizontalAlignment(JLabel.CENTER);
        controlPanel.add(statusLabel, BorderLayout.SOUTH);

        add(controlPanel, BorderLayout.SOUTH);

        updateBoard();
        revalidate();
        repaint();
    }

    // Trova una porta disponibile per il server
    private int findAvailablePort() {
        Random random = new Random();
        while (true) {
            int randomPort = random.nextInt(16384) + 49152;
            try {
                serverSocket = new ServerSocket(randomPort);
                return randomPort;
            } catch (IOException e) {
                // Porta non disponibile, prova la prossima
            }
        }
    }

    // Aggiorna la visualizzazione della tavola di gioco
    private void updateBoard() {
        SwingUtilities.invokeLater(() -> {
            for (int i = 0; i < 8; i++) {
                for (int j = 0; j < 8; j++) {
                    cells[i][j].removeAll();
                    char piece = board.getPiece(i, j);
                    if (piece != '-') {
                        JPanel piecePanel = new JPanel();
                        piecePanel.setPreferredSize(new Dimension(40, 40));
                        piecePanel.setBackground(piece == 'B' ? Color.BLACK : Color.WHITE);
                        piecePanel.setBorder(BorderFactory.createLineBorder(Color.BLACK, 1));
                        cells[i][j].add(piecePanel);
                    }
                    cells[i][j].revalidate();
                    cells[i][j].repaint();
                }
            }
            updateStatusLabel();
        });
    }

    // Gestisce una mossa del giocatore
    private void makeMove(int row, int col) {
        if (myTurn && board.isValidMove(row, col, myColor)) {
            board.makeMove(row, col, myColor);
            updateBoard();

            if (playingVsComputer) {
                myTurn = false;
                Timer timer = new Timer(500, e -> makeComputerMove());
                timer.setRepeats(false);
                timer.start();
            } else {
                sendMove(row, col);
                myTurn = false;
            }

            checkGameOver();

            if (!playingVsComputer && !hasValidMove((myColor == 'B') ? 'W' : 'B')) {
                out.println("NO_MOVE");
            }
        }
    }

    // Esegue una mossa del computer
    private void makeComputerMove() {
        ArrayList<int[]> validMoves = new ArrayList<>();
        for (int i = 0; i < 8; i++) {
            for (int j = 0; j < 8; j++) {
                if (board.isValidMove(i, j, 'W')) {
                    validMoves.add(new int[]{i, j});
                }
            }
        }
        if (!validMoves.isEmpty()) {
            int[] move = validMoves.get(new Random().nextInt(validMoves.size()));
            board.makeMove(move[0], move[1], 'W');
            updateBoard();
        } else {
            JOptionPane.showMessageDialog(this, "Il computer non ha mosse disponibili. È il tuo turno.");
        }
        myTurn = true;
        checkGameOver();
    }

    // Invia una mossa all'avversario
    private void sendMove(int row, int col) {
        out.println("MOVE " + row + " " + col);
    }

    // Gestisce l'azione di resa o abbandono
    private void handleSurrender() {
        if (playingVsComputer) {
            askForNewGame();
        } else if (out != null) {
            out.println("SURRENDER");
            JOptionPane.showMessageDialog(this, "Ti sei arreso!");
            showStartPanel();
        }
    }

    // Funzione di controllo vittoria-sconfitta dei giocatori
    private void checkGameOver() {
        int blackCount = board.countPieces('B');
        int whiteCount = board.countPieces('W');
        int emptyCells = 64 - blackCount - whiteCount;

        if (!hasValidMove('B') && !hasValidMove('W')) {
            String result;
            String winnerMessage;
            boolean playerWon;

            if (blackCount > whiteCount) {
                result = "Il nero ha vinto!";
                playerWon = (myColor == 'B');
            } else if (whiteCount > blackCount) {
                result = "Il bianco ha vinto!";
                playerWon = (myColor == 'W');
            } else {
                result = "Pareggio!";
                playerWon = false;
            }

            winnerMessage = playerWon ? "Hai vinto!" : (result.equals("Pareggio!") ? "Pareggio!" : "Hai perso!");

            if (emptyCells > 0) {
                winnerMessage += String.format(" (Rimaste %d caselle vuote)", emptyCells);
            }

            JOptionPane.showMessageDialog(this, winnerMessage);

            if (!playingVsComputer && out != null) {
                String opponentMessage = playerWon ? "Hai perso!" : (result.equals("Pareggio!") ? "Pareggio!" : "Hai vinto!");
                if (emptyCells > 0) {
                    opponentMessage += String.format(" (Rimaste %d caselle vuote)", emptyCells);
                }
                out.println("GAME_OVER " + opponentMessage);
            }

            askForNewGame();
        }
    }

    // Chiede al giocatore se vuole giocare di nuovo
    private void askForNewGame() {
        int choice = JOptionPane.showConfirmDialog(this,
                "Vuoi giocare di nuovo?", "Partita Terminata",
                JOptionPane.YES_NO_OPTION);
        if (choice == JOptionPane.YES_OPTION) {
            if (playingVsComputer) {
                startGameVsComputer();
            } else if (isServer) {
                restartServer();
            } else {
                reconnectAsClient();
            }
        } else {
            if (!playingVsComputer && out != null) {
                out.println("DECLINE_PLAY_AGAIN");
            }
            showStartPanel();
        }
    }

    // Riavvia il server per una nuova partita
    private void restartServer() {
        try {
            if (serverSocket != null && !serverSocket.isClosed()) {
                serverSocket.close();
            }
            if (socket != null && !socket.isClosed()) {
                socket.close();
            }
            startGame(true);
        } catch (IOException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, "Errore nel riavvio del server. Tornando al menu principale.");
            showStartPanel();
        }
    }

    // Riconnette come client per una nuova partita
    private void reconnectAsClient() {
        try {
            if (socket != null && !socket.isClosed()) {
                socket.close();
            }
            startGame(false);
        } catch (IOException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, "Errore nella riconnessione. Tornando al menu principale.");
            showStartPanel();
        }
    }

    // Aggiorna l'etichetta di stato con il conteggio dei pezzi
    private void updateStatusLabel() {
        int blackCount = board.countPieces('B');
        int whiteCount = board.countPieces('W');
        statusLabel.setText("Nero: " + blackCount + " | Bianco: " + whiteCount);
    }

    // Verifica se un giocatore ha mosse valide
    private boolean hasValidMove(char color) {
        for (int i = 0; i < 8; i++) {
            for (int j = 0; j < 8; j++) {
                if (board.isValidMove(i, j, color)) {
                    return true;
                }
            }
        }
        return false;
    }

    // Classe interna per gestire gli eventi del mouse sulle celle della tavola
    private class MoveListener extends MouseAdapter {
        private int row, col;

        public MoveListener(int row, int col) {
            this.row = row;
            this.col = col;
        }

        @Override
        public void mouseClicked(MouseEvent e) {
            makeMove(row, col);
        }
    }

    // Classe interna per gestire il server
    private class Server extends Thread {
        @Override
        public void run() {
            try {
                socket = serverSocket.accept();
                out = new PrintWriter(socket.getOutputStream(), true);
                in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

                String inputLine;
                while ((inputLine = in.readLine()) != null) {
                    processInput(inputLine);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    // Classe interna per gestire il client
    private class Client extends Thread {
        @Override
        public void run() {
            try {
                socket = new Socket(ipAddress, port);
                out = new PrintWriter(socket.getOutputStream(), true);
                in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

                String inputLine;
                while ((inputLine = in.readLine()) != null) {
                    processInput(inputLine);
                }
            } catch (IOException e) {
                JOptionPane.showMessageDialog(ReversiGame.this,
                        "Errore di connessione: " + e.getMessage() + "\n" +
                                "Assicurati che:\n" +
                                "1. Il server sia in esecuzione\n" +
                                "2. L'indirizzo IP e la porta siano corretti\n" +
                                "3. Entrambi i computer siano sulla stessa rete");
                showStartPanel();
            }
        }
    }

    // Processa i messaggi in arrivo  dall'altro giocatore
    private void processInput(String input) {
        if (input.startsWith("MOVE")) {
            String[] parts = input.split(" ");
            int row = Integer.parseInt(parts[1]);
            int col = Integer.parseInt(parts[2]);

            board.makeMove(row, col, (myColor == 'B') ? 'W' : 'B');
            updateBoard();

            myTurn = true;

            if (!hasValidMove(myColor)) {
                out.println("NO_MOVE");
                myTurn = false;
                JOptionPane.showMessageDialog(this, "Non hai mosse disponibili. È ancora il turno dell'avversario.");
            }

            checkGameOver();

        } else if (input.equals("SURRENDER")) {
            JOptionPane.showMessageDialog(this, "L'avversario si è arreso. Hai vinto!");
            askForNewGame();
        } else if (input.equals("NO_MOVE")) {
            if (hasValidMove(myColor)) {
                myTurn = true;
                JOptionPane.showMessageDialog(this, "L'avversario non ha mosse disponibili. È il tuo turno.");
            } else {
                JOptionPane.showMessageDialog(this, "Nessun giocatore ha mosse disponibili. La partita è terminata.");
                checkGameOver();
            }
        } else if (input.startsWith("GAME_OVER")) {
            String message = input.substring("GAME_OVER ".length());
            JOptionPane.showMessageDialog(this, message);
            askForNewGame();
        } else if (input.equals("PLAY_AGAIN")) {
            int choice = JOptionPane.showConfirmDialog(this,
                    "L'avversario vuole giocare di nuovo. Accetti?", "Nuova Partita",
                    JOptionPane.YES_NO_OPTION);
            if (choice == JOptionPane.YES_OPTION) {
                resetGame();
            } else {
                out.println("DECLINE_PLAY_AGAIN");
                showStartPanel();
            }
        } else if (input.equals("DECLINE_PLAY_AGAIN")) {
            JOptionPane.showMessageDialog(this, "L'avversario non vuole giocare di nuovo.");
            showStartPanel();
        }
    }

    // Resetta il gioco per una nuova partita
    private void resetGame() {
        board = new Board();
        myTurn = (myColor == 'B');
        updateBoard();
    }

    // Metodo main per avviare l'applicazione
    public static void main(String[] args) {
        SwingUtilities.invokeLater(ReversiGame::new);
    }
}
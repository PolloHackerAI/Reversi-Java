import java.io.*;
import java.net.*;

class NetworkManager {
    private Socket socket;
    private ServerSocket serverSocket;
    private PrintWriter out;
    private BufferedReader in;

    public void setupServer(int port) throws IOException {
        serverSocket = new ServerSocket(port);
        socket = serverSocket.accept();
        setupStreams();
    }

    public void setupClient(String ipAddress, int port) throws IOException {
        socket = new Socket(ipAddress, port);
        setupStreams();
    }

    private void setupStreams() throws IOException {
        out = new PrintWriter(socket.getOutputStream(), true);
        in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
    }

    public void sendMove(int row, int col) {
        out.println("MOVE " + row + " " + col);
    }

    public void sendSurrender() {
        out.println("SURRENDER");
    }

    public String receiveMessage() throws IOException {
        return in.readLine();
    }

    public void close() {
        try {
            if (out != null) out.close();
            if (in != null) in.close();
            if (socket != null) socket.close();
            if (serverSocket != null) serverSocket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void waitForClient() throws IOException {
        socket = serverSocket.accept();
        setupStreams();
    }
}

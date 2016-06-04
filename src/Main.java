import org.java_websocket.server.WebSocketServer;

import java.net.InetSocketAddress;

public class Main {
    public static void main(String[] args) {
        String host = "0.0.0.0";
        int port = 9001;
        Server server = new Server(new InetSocketAddress(host, port));
        UpdateThread thread = new UpdateThread(server);
        thread.start();
        server.run();
    }
}

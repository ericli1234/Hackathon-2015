import org.java_websocket.WebSocket;

public class Connection {
    public WebSocket socket;
    public int id;
    public double x = 350;
    public double y = 350;
    public double vx = 0;
    public double vy = 0;
    public double size = 10;
    public double speed = 1;
    public boolean keyUp;
    public boolean keyDown;
    public boolean keyLeft;
    public boolean keyRight;
    public boolean connected = false;
    public Connection(WebSocket socket) {
        this.socket = socket;
    }
}

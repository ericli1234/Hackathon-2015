import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;
import org.json.JSONObject;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class Server extends WebSocketServer {

    public HashMap<WebSocket, Connection> connections = new HashMap<WebSocket, Connection>();
    public ArrayList<Bullet> bullets = new ArrayList<Bullet>();
    public ArrayList<Wall> walls = new ArrayList<Wall>();

    public static final double bulletSpeed = 20;
    public static final double massLossBullet = 0;
    public static final double massLossWall = 0;
    public static final double minMassBullet = 10;
    public static final double minMassWall = 10;

    public double gameRadius = 5000;

    public Server(InetSocketAddress address) {
        super(address);
    }

    public void onOpen(WebSocket webSocket, ClientHandshake clientHandshake) {
        synchronized(connections) {
            Connection connection = new Connection(webSocket);
            loop:
            for(int i = 0; i < 4; ++i) {
                for (Map.Entry<WebSocket, Connection> c : connections.entrySet()) {
                    if(c.getValue().id == i) {
                        continue loop;
                    }
                }
                connection.id = i;
                connections.put(webSocket, connection);
                return;
            }
        }
    }

    public void onClose(WebSocket webSocket, int i, String s, boolean b) {
        synchronized(connections) {
            Connection c = connections.get(webSocket);
            Iterator<Wall> iterator = walls.iterator();
            while(iterator.hasNext()) {
                if(iterator.next().id == c.id) {
                    iterator.remove();
                }
            }
            connections.remove(webSocket);
            System.out.println("closed: " + webSocket.getRemoteSocketAddress());
        }
    }

    public void onMessage(WebSocket webSocket, String s) {
//        System.out.println(s);
        synchronized(connections) {
            Connection connection = connections.get(webSocket);
            JSONObject object = new JSONObject(s);
            int type = object.getInt("type");
            switch (type) {
                case 7:
                    connection.connected = true;
                    webSocket.send("{\"type\": 6, \"id\": " + connection.id + ", \"radius\": " + gameRadius + "}");
                    break;
                case 12:
                    String str = object.getString("direction");
                    int state = object.getInt("keyState");
                    if(str.equals("up")) {
                        connection.keyUp = state == 1;
                    } else if(str.equals("down")) {
                        connection.keyDown = state == 1;
                    } else if(str.equals("left")) {
                        connection.keyLeft = state == 1;
                    } else if(str.equals("right")) {
                        connection.keyRight = state == 1;
                    }
                    break;
                case 13:
                    double x = object.getInt("x");
                    double y = object.getInt("y");
                    double dx = x - connection.x;
                    double dy = y - connection.y;
                    double l = Math.sqrt(dx * dx + dy * dy);
                    dx /= l;
                    dy /= l;
                    if(connection.size > minMassBullet) {
                        bullets.add(new Bullet(connection.x + dx * connection.size, connection.y + dy * connection.size, dx * bulletSpeed, dy * bulletSpeed));
                        connection.size -= massLossBullet;
                    }
                    break;
                case 14:
                    x = object.getInt("x");
                    y = object.getInt("y");
                    if(connection.size > minMassWall) {
                        boolean add = true;
                        for(int i = 0; i < walls.size(); ++i) {
                            Wall w = walls.get(i);
                            double xd = x - w.x;
                            double yd = y - w.y;
                            double d = Math.sqrt(xd * xd + yd * yd);
                            if(d < 16) {
                                add = false;
                            }
                        }
                        if(x < 8 || x > gameRadius - 8 || y < 8 || y > gameRadius - 8) {
                            add = false;
                        }
                        for(Map.Entry<WebSocket, Connection> e : connections.entrySet()) {
                            Connection c = e.getValue();
                            double xd = x - c.x;
                            double yd = y - c.y;
                            double d = Math.sqrt(xd * xd + yd * yd);
                            if(d < c.size + 8) {
                                add = false;
                            }
                        }
                        if(add) {
                            walls.add(new Wall(x, y, connection.id));
                            connection.size -= massLossWall;
                        }
                    }
                    break;
            }
        }
    }

    public void onError(WebSocket webSocket, Exception e) {
        System.out.println("errored: " + e);
    }
}

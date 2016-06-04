import org.java_websocket.WebSocket;

import java.util.Iterator;
import java.util.Map;

public class UpdateThread extends Thread {
    public Server server;
    public UpdateThread(Server server) {
        this.server = server;
    }
    public void run() {
        while(true) {
            synchronized (server.connections) {
                Iterator<Bullet> iterator = server.bullets.iterator();
                while(iterator.hasNext()) {
                    Bullet b = iterator.next();
                    b.x += b.vx;
                    b.y += b.vy;
                    if(b.x > server.gameRadius || b.y > server.gameRadius || b.x < 0 || b.y < 0) {
                        iterator.remove();
                        continue;
                    }
                    Iterator<Wall> wallIter = server.walls.iterator();
                    boolean delete = false;
                    while(wallIter.hasNext()) {
                        Wall w = wallIter.next();
                        double dx = w.x - b.x;
                        double dy = w.y - b.y;
                        double d = Math.sqrt(dx * dx + dy * dy);
                        if(d < 12) {
                            wallIter.remove();
                            delete = true;
                            break;
                        }
                    }
                    if(delete) {
                        iterator.remove();
                        continue;
                    }
                }
                for(Map.Entry<WebSocket, Connection> entry : server.connections.entrySet()) {
                    Connection c = entry.getValue();
                    if(c.keyUp) {
                        c.vy -= c.speed;
                    }
                    if(c.keyDown) {
                        c.vy += c.speed;
                    }
                    if(c.keyLeft) {
                        c.vx -= c.speed;
                    }
                    if(c.keyRight) {
                        c.vx += c.speed;
                    }
                    c.x += c.vx;
                    c.y += c.vy;
                    c.size += 0.2;
                    c.size = c.size * 0.995;
                    if(c.x + c.size > server.gameRadius) {
                        c.x = server.gameRadius - c.size;
                        c.vx = 0;
                    }
                    else if(c.x - c.size < 0) {
                        c.x = c.size;
                        c.vx = 0;
                    }
                    if(c.y + c.size > server.gameRadius) {
                        c.y = server.gameRadius - c.size;
                        c.vy = 0;
                    }
                    else if(c.y - c.size < 0) {
                        c.y = c.size;
                        c.vy = 0;
                    }
                    boolean collided = false;
                    for(int i = 0; i < server.walls.size(); ++i) {
                        Wall w = server.walls.get(i);
                        double dx = w.x - c.x;
                        double dy = w.y - c.y;
                        double d = Math.sqrt(dx * dx + dy * dy);
                        if(c.size + 8 > d && c.id != w.id) {
                            double nx = dx * (8 + c.size) / d;
                            double ny = dy * (8 + c.size) / d;
                            c.x = w.x - nx;
                            c.y = w.y - ny;
                            collided = true;
                        }
                    }
                    if(collided) {
                        c.vx = 0;
                        c.vy = 0;
                    }
                    Iterator<Bullet> iter = server.bullets.iterator();
                    while(iter.hasNext()) {
                        Bullet b = iter.next();
                        double xd = b.x - c.x;
                        double yd = b.y - c.y;
                        double d = Math.sqrt(xd * xd + yd * yd);
                        if(4 + c.size > d) {
                            iter.remove();
                            c.size -= 5;
                        }
                    }
                }
                for(Map.Entry<WebSocket, Connection> entry : server.connections.entrySet()) {
                    Connection c = entry.getValue();
                    if (c.size < 5) {
                        c.size = 0;
                        c.socket.send("{\"type\": 69}");
                    }
                }

                String s = "";
                for(Map.Entry<WebSocket, Connection> entry : server.connections.entrySet()) {
                    Connection connection = entry.getValue();
                    s += "{\"obj\": 0, \"id\": " + connection.id + ", \"x\": " + connection.x + ", \"y\": " + connection.y + ", \"size\": " + connection.size + "}, ";
                }
                for(Bullet b : server.bullets) {
                    s += "{\"obj\": 2, \"id\": 4, \"x\": " + b.x + ", \"y\": " + b.y + ", \"size\": 0}, ";
                }
                for(Wall w : server.walls) {
                    s += "{\"obj\": 1, \"id\": " + w.id + ", \"x\": " + w.x + ", \"y\": " + w.y + ", \"size\": 0}, ";
                }
                if(!s.isEmpty()) {
                    s = s.substring(0, s.length() - 2);
                }
                for(Map.Entry<WebSocket, Connection> entry : server.connections.entrySet()) {
                    Connection connection = entry.getValue();
                    connection.socket.send("{\"type\": 15, \"data\": [" + s + "]}");
                }
            }
            try{
                Thread.sleep(50);
            } catch(InterruptedException e) {}
        }
    }
}

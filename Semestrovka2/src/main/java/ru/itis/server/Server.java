package ru.itis.server;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

public class Server extends Thread {
    public List<Connection> connections =
            Collections.synchronizedList(new ArrayList<>());
    public List<GameRoom> roomList = new ArrayList<>();
    public ServerSocket server;
    boolean run = true;
    int connectionId = 0;

    @Override
    public void run(){
        try {
            server = new ServerSocket(7778);

            while (run) {
                Socket socket = server.accept();
                Connection con = new Connection(socket, this);
                con.connectionId = connectionId;
                connectionId++;
                connections.add(con);
                con.start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            closeAll();
        }
    }

    public Server() {
    }

    public boolean roomIsCreat(int number) {
        boolean isCreate = false;
        for (GameRoom room : roomList) {
            if (room.number == number) isCreate = true;
        }
        return isCreate;
    }

    public GameRoom connectToRoom(int number) {
        for (GameRoom room : roomList) {
            if (room.number == number) {
                return room;
            }
        }
        return null;
    }

    public GameRoom createRoom(int number) {
        GameRoom room = new GameRoom(number);
        roomList.add(room);
        return room;
    }

    public void closeAll() {
        try {
            server.close();
            run = false;

            synchronized (connections) {
                Iterator<Connection> iter = connections.iterator();
                Iterator<GameRoom> iterRoom = roomList.iterator();
                while (iter.hasNext()) {
                    iter.next().close(true);
                }
                while (iterRoom.hasNext()) {
                    iterRoom.next().close(true);
                }
            }
        } catch (Exception e) {
            System.err.println("Error close");
        }
    }

}

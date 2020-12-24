package ru.itis.server;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Iterator;
import java.util.Objects;

public class Connection extends Thread {
    public boolean isReady = false;
    private BufferedReader in;
    public PrintWriter out;
    public Socket socket;
    private GameRoom curRoom;
    private Server server;
    private boolean isWorking = true;
    private String name = "";
    private int room = 0;
    private Connection firstPlayer;
    private Connection secondPlayer;
    public int connectionId = 0;

    public Connection(Socket socket, Server server) {
        this.socket = socket;
        this.server = server;

        try {
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            out = new PrintWriter(socket.getOutputStream(), true);

        } catch (IOException e) {
            e.printStackTrace();
            close(true);
        }
    }

    public void run() {
        try {
            name = in.readLine();
            try {
                room = Integer.parseInt(in.readLine());
            } catch (Exception e) {
                out.println("Неправильный номер");
                this.close(true);
            }
            if (server.roomIsCreat(room)) curRoom = server.connectToRoom(room);
            else curRoom = server.createRoom(room);
            if (isWorking) {
                if (curRoom != null) {
                    if (curRoom.user.size() < 2) {
                        curRoom.user.add(this);
                        synchronized (curRoom) {
                            for (Connection connection : curRoom.user) {
                                connection.out.println("3" + name + " теперь в комнате");
                            }
                        }
                    } else {
                        out.println("Комната заполнена");
                        close(true);
                    }
                } else {
                    out.println("Комната не существует");
                    close(true);
                }
            }

            String str = "";
            while (true) {
                str = in.readLine();
                if (str.equals("canStart")) {
                    isReady = true;
                    synchronized (curRoom) {
                        for (Connection connection : curRoom.user) {
                            connection.out.println("3" + name + " готов");
                        }
                    }
                    if (checkAllReady()) {
                        startGame();
                    }
                } else {
                    if (firstPlayer == null || secondPlayer == null) setPlayer();
                    String whoSent = str.charAt(0) + "";
                    str = str.substring(1);
                    boolean firstPlayerBool;
                    if (whoSent.equals("f")) {
                        firstPlayerBool = true;
                    } else firstPlayerBool = false;
                    if (str.equals("\\q")) break;
                    synchronized (curRoom) {
                        if (firstPlayerBool) {
                            secondPlayer.out.println(str);
                        } else {
                            firstPlayer.out.println(str);
                        }
                    }
                }
            }
            synchronized (curRoom) {
                Iterator<Connection> iter = curRoom.user.iterator();
                while (iter.hasNext()) {
                    ((Connection) iter.next()).out.println("3" + name + " больше не с нами");
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            close(true);
        }
    }

    public String getUsername(){
        return name;
    }
    public int getRoomNum(){
        return room;
    }
    public GameRoom getRoom(){
        return curRoom;
    }

    private boolean checkAllReady() {
        boolean allReady = true;
        if (curRoom.user.size() == 2) {
            Iterator<Connection> iter = curRoom.user.iterator();
            while (iter.hasNext()) {
                if (!((Connection) iter.next()).isReady) allReady = false;
            }
        } else allReady = false;
        return allReady;
    }

    private void startGame() {
        setPlayer();
        firstPlayer.out.println("41");//4 - начало игры \ 1 - начинает первым
        secondPlayer.out.println("42");  //4 - начало игры \ 2 - начинает вторым

    }

    public void setPlayer() {
        Iterator<Connection> iter = curRoom.user.iterator();
        int count = 0;
        while (iter.hasNext()) {
            if (count == 0) {
                firstPlayer = iter.next();
            } else if (count == 1) {
                secondPlayer = iter.next();
            }
            count++;
        }
    }

    public void close(boolean fullclose) {
        try {
            if(curRoom!= null) {
                curRoom.user.remove(this);
                if (curRoom.user.size() == 0) server.roomList.remove(curRoom);
            }


            if (fullclose) {
                isWorking = false;
                out.println("f\\q");
                socket.close();
                server.connections.remove(this);
            }
            System.out.println("Close connection");
            if (server.connections.size() == 0) {
                server.closeAll();
            }
            in.close();
            out.close();
        } catch (Exception e) {
            System.err.println("Траблы с закрытием");
        }
        finally {
            server.connections.remove(this);
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Connection that = (Connection) o;
        return ((Connection) o).connectionId == that.connectionId;
    }

    @Override
    public int hashCode() {
        return Objects.hash(server);
    }
}
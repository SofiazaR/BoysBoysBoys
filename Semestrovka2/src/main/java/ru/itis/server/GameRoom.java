package ru.itis.server;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class GameRoom {
    public int number;
    public List<Connection> user = new ArrayList();
    public boolean gameIsStart = false;

    public GameRoom(int number) {
        this.number = number;
    }

    public boolean checkRoom(){
        if(user.size() == 2) return true;
        else return false;
    }

    public void close(boolean b) {
        Iterator<Connection> iter = user.iterator();
        while(iter.hasNext()) {
            ((Connection) iter.next()).close(true);
        }
    }
}

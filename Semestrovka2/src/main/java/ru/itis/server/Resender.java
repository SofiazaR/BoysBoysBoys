package ru.itis.server;

import ru.itis.application.Main;

import java.io.IOException;

public class Resender extends Thread {
    Main game;
    private boolean stoped;

    public Resender(Main game) {
        this.game = game;
    }

    public Resender() {
    }

    public void setStop() {
        stoped = true;
    }

    @Override
    public void run() {
        try {
            while (!stoped) {
                String str = game.in.readLine();
                game.getMessage(str);
            }
        } catch (IOException e) {
            System.err.println("Ошибка при получении сообщения.");
            e.printStackTrace();
        }
    }
}

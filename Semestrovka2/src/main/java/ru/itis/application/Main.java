package ru.itis.application;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Point2D;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.GridPane;
import javafx.scene.paint.Color;
import javafx.scene.text.Text;
import javafx.stage.Stage;
import ru.itis.server.Resender;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

public class Main extends Application {

    public Stage stage;

    public Socket socket;
    public BufferedReader in;
    public PrintWriter out;
    public Resender resend;
    @FXML
    public GridPane box_player;
    @FXML
    public GridPane box_enemy;
    @FXML
    public TextField tf_roomNum;
    @FXML
    public Button btn_connectToTheRoom;
    @FXML
    public TextArea ta_gameInfoPanel;
    @FXML
    public TextField tf_usernameInput;
    public Button btn_restart;
    public TextField tf_ipPort;
    public Text text_enemyFieldInfo;
    public Text text_playerFieldInfo;
    public Text text_gameInfo;
    public TextField tf_chatInput;
    public Button btn_sendChatMessage;
    int ship4 = 1;
    int ship3 = 2;
    int ship2 = 3;
    int ship1 = 4;
    final int MAX_ENEMY_POINT = ship4 * 4 + ship3 * 3 + ship2 * 2 + ship1;
    int enemyPoint = 0;
    Cell lastCell;
    boolean isFirstPlayer = false;
    private String ip;
    private int port;
    private boolean isRun;
    private boolean enemyTurn = true;
    private String username;

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) throws Exception {
        createContent();
    }

    private void createContent() {
        AnchorPane root;
        stage = new Stage();
        FXMLLoader loader;
        try {
            loader = new FXMLLoader(getClass().getResource("/gameBoard.fxml"));
            root = loader.load();
            stage.setTitle("client");
            Image icon = new Image("/icon.png");
            stage.getIcons().add(icon);
            stage.setScene(new Scene(root, 714, 627));
            stage.setResizable(false);
            stage.sizeToScene();
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
        stage.show();

    }

    private void setDisabledConnect(boolean disable) {
        btn_connectToTheRoom.setDisable(disable);
        tf_ipPort.setEditable(!disable);
        tf_usernameInput.setEditable(!disable);
        tf_roomNum.setEditable(!disable);
    }

    private void setVisibleText(boolean visible) {
        text_enemyFieldInfo.setVisible(visible);
        text_playerFieldInfo.setVisible(visible);
        text_gameInfo.setVisible(visible);
    }


    private void checkMessage(String message) {
        // 0 - ход| 1 - проигрыш  | 3 - инфа,чат | 4 - начало игры
        String action = message.charAt(0) + "";
        String info;
        switch (action) {
            case "1": {
                gameOver();
                break;
            }
            case "0": {
                info = message.substring(1);
                getStep(info);
                break;
            }
            case "3": {
                info = message.substring(1);
                showMessage(info);
                break;
            }
            case "4": {
                info = message.substring(1);
                startGame(info);
                break;
            }
            default: {
                hasError();
            }
        }
    }

    private void startGame(String info) {
        ta_gameInfoPanel.textProperty().addListener((ChangeListener<Object>) (observable, oldValue, newValue) -> ta_gameInfoPanel.setScrollTop(Double.MAX_VALUE));

        isRun = true;
        if (info.equals("1")) {
            showMessage("Вы начинаете первым");
            isFirstPlayer = true;
            startMove();
        } else {
            endMove();
            showMessage("Вы начинаете вторым");
        }
        hideChat(true);
    }

    private void showMessage(String info) {
        ta_gameInfoPanel.appendText("\n" + info);
    }

    private void hideChat(boolean hide){
        tf_chatInput.setVisible(hide);
        btn_sendChatMessage.setVisible(hide);
    }

    public void startConnection() {
        try {
            String[] inputDataToConnect = tf_ipPort.getText().split(":");
            ip = inputDataToConnect[0];
            port = Integer.parseInt(inputDataToConnect[1]);
            socket = new Socket(ip, port);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            out = new PrintWriter(socket.getOutputStream(), true);
            resend = new Resender(this);
            setDisabledConnect(true);
            username = tf_usernameInput.getText().trim();
            out.println(username);
            out.println(tf_roomNum.getText());
            resend.start();
            createBoard();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    private void createBoard() {
        Platform.runLater(() -> {
            EventHandler<? super MouseEvent> enemyHandler = (EventHandler<MouseEvent>) event -> {
                if (isRun) {
                    Cell cell = (Cell) event.getSource();
                    if (event.getButton() == MouseButton.SECONDARY) {
                        if (!cell.wasShot) {
                            if (cell.marked) {
                                cell.marked = false;
                                cell.setFill(Color.AQUA);
                            }
                            else {
                                cell.marked = true;
                                cell.setFill(Color.LIGHTGRAY);
                            }
                        }
                    } else {
                        cell.shoot();
                        sendMessage("0shoot;" + cell.x + ";" + cell.y);
                        lastCell = cell;
                    }
                }
            };

            EventHandler<? super MouseEvent> playerHandler = (EventHandler<MouseEvent>) event -> {
                if (!isRun) {
                    boolean verticalShip = false;
                    if (event.getButton() == MouseButton.PRIMARY || event.getButton() == MouseButton.SECONDARY) {

                        if (event.getButton() == MouseButton.SECONDARY) verticalShip = true;

                        if (ship4 != 0) {
                            Cell cell = (Cell) event.getSource();
                            if (placeShip(new Ship(4, verticalShip), cell.x, cell.y)) ship4--;
                        } else if (ship3 != 0) {
                            Cell cell = (Cell) event.getSource();
                            if (placeShip(new Ship(3, verticalShip), cell.x, cell.y)) ship3--;
                        } else if (ship2 != 0) {
                            Cell cell = (Cell) event.getSource();
                            if (placeShip(new Ship(2, verticalShip), cell.x, cell.y)) ship2--;
                        } else if (ship1 != 0) {
                            Cell cell = (Cell) event.getSource();
                            if (placeShip(new Ship(1, verticalShip), cell.x, cell.y)) ship1--;
                            if (ship1 == 0) {
                                box_player.setDisable(true);
                                sendCanStart();
                            }
                        }
                    }
                }
            };

            for (int y = 0; y < 10; y++) {
                for (int x = 0; x < 10; x++) {
                    Cell e1 = new Cell(x, y, true);
                    Cell p1 = new Cell(x, y, false);
                    e1.setOnMouseClicked(enemyHandler);
                    p1.setOnMouseClicked(playerHandler);
                    box_player.add(p1, y, x);
                    box_enemy.add(e1, y, x);
                }
            }
            disableInterface(false);
            setVisibleText(true);
            setGameInfo("Выставьте корабли", Color.BLACK);
        });

    }

    private void sendCanStart() {
        setGameInfo("Ожидайте начала игры", Color.BLACK);
        out.println("canStart");
    }

    private void setGameInfo(String text, Color color) {
        text_gameInfo.setText(text);
        text_gameInfo.setFill(color);
    }

    public void sendMessage(String message) {
        if (isFirstPlayer) message = "f" + message;
        else message = "s" + message;
        out.println(message);
    }

    public void startMove() {
        enemyTurn = false;
        box_enemy.setDisable(false);
        setGameInfo("Ваш ход", Color.GREEN);
    }

    public void endMove() {
        enemyTurn = true;
        box_enemy.setDisable(true);
        setGameInfo("Ход противника", Color.RED);
    }

    public void getMessage(String message) {
        checkMessage(message);
    }

    private void hasError() {
        System.exit(0);
    }

    private void getStep(String info) {
        String[] coords = info.split(";");
        switch (coords[0]) {
            case "shoot":
                if (enemyTurn) {
                    int x = Integer.parseInt(coords[1]);
                    int y = Integer.parseInt(coords[2]);
                    Cell cell = getCell(box_player, x, y);
                    if (!cell.shoot()) {
                        startMove();
                        sendMessage("0endMove");
                    } else {
                        sendMessage("0continue");
                    }
                } else {
                    int x = Integer.parseInt(coords[1]);
                    int y = Integer.parseInt(coords[2]);
                    Cell cell = getCell(box_enemy, x, y);
                    cell.shoot();

                }
                break;
            case "endMove":
                endMove();
                break;
            case "continue":
                enemyPoint++;
                lastCell.setFill(Color.RED);
                setGameInfo("Вы попали", Color.GREEN);
                if (enemyPoint == MAX_ENEMY_POINT) {
                    disableInterface(true);
                    showMessage("Вы победили");
                    setGameInfo("Вы победили", Color.GREEN);
                    sendMessage("1");
                    btn_restart.setVisible(true);
                }
                break;
        }
    }

    public Cell getCell(GridPane from, int x, int y) {
        return (Cell) from.getChildren().get(y * 10 + x);
    }


    private void gameOver() {
        disableInterface(true);
        showMessage("Вы проиграли");
        btn_restart.setVisible(true);
        setGameInfo("Игра окончена", Color.BLACK);
    }

    private void disableInterface(boolean disable) {
        box_player.setDisable(disable);
        box_enemy.setDisable(disable);
    }

    public void disconnect() {
        out.println("f\\q");
        resend.setStop();
        try {
            socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private Cell[] getNeighbors(int x, int y) {
        Point2D[] points = new Point2D[]{
                new Point2D(x - 1, y),
                new Point2D(x + 1, y),
                new Point2D(x, y - 1),
                new Point2D(x, y + 1),
                new Point2D(x + 1, y + 1),
                new Point2D(x - 1, y + 1),
                new Point2D(x + 1, y - 1),
                new Point2D(x - 1, y - 1)
        };
        List<Cell> neighbors = new ArrayList<Cell>();

        for (Point2D p : points) {
            if (isValidPoint(p)) {
                neighbors.add(getCell(box_player, (int) p.getX(), (int) p.getY()));
            }
        }

        return neighbors.toArray(new Cell[0]);
    }

    private boolean isValidPoint(Point2D point) {
        return isValidPoint(point.getX(), point.getY());
    }

    private boolean isValidPoint(double x, double y) {
        return x >= 0 && x < 10 && y >= 0 && y < 10;
    }

    private boolean canPlaceShip(Ship ship, int x, int y) {
        int length = ship.type;

        if (ship.vertical) {
            for (int i = y; i < y + length; i++) {
                if (!isValidPoint(x, i))
                    return false;

                Cell cell = getCell(box_player, x, i);
                if (cell.ship != null)
                    return false;

                for (Cell neighbor : getNeighbors(x, i)) {

                    if (neighbor.ship != null)
                        return false;
                }
            }
        } else {
            for (int i = x; i < x + length; i++) {
                if (!isValidPoint(i, y))
                    return false;

                Cell cell = getCell(box_player, i, y);
                if (cell.ship != null)
                    return false;

                for (Cell neighbor : getNeighbors(i, y)) {

                    if (neighbor.ship != null)
                        return false;
                }
            }
        }

        return true;
    }

    public boolean placeShip(Ship ship, int x, int y) {
        if (canPlaceShip(ship, x, y)) {
            int length = ship.type;

            if (ship.vertical) {
                for (int i = y; i < y + length; i++) {
                    Cell cell = getCell(box_player, x, i);
                    cell.ship = ship;
                    cell.setFill(Color.DARKGRAY);
                    cell.setStroke(Color.BLACK);

                }
            } else {
                for (int i = x; i < x + length; i++) {
                    Cell cell = getCell(box_player, i, y);
                    cell.ship = ship;
                    cell.setFill(Color.DARKGRAY);
                    cell.setStroke(Color.BLACK);

                }
            }

            return true;
        }

        return false;
    }

    public void restartWindows() {
        disconnect();
        setDisabledConnect(false);
        resend.setStop();
        ship4 = 1;
        ship3 = 2;
        hideChat(false);
        ship2 = 3;
        ship1 = 4;
        ta_gameInfoPanel.setText("");
        lastCell = null;
        isRun = false;
        disableInterface(false);
        isFirstPlayer = false;
        enemyPoint = 0;
        clearBoard();
        enemyTurn = true;
        btn_restart.setVisible(false);
    }

    private void clearBoard() {
        box_enemy.getChildren().clear();
        box_player.getChildren().clear();
        setVisibleText(false);
    }

    public void sendChatMessage() {
        String msg = tf_chatInput.getText();
        if(!msg.isEmpty() && isRun){
            msg = "3" + username + ": " + msg;
            tf_chatInput.setText("");
            checkMessage(msg);
            sendMessage(msg);
        }
    }

}

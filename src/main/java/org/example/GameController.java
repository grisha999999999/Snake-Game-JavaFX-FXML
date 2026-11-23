package org.example;

import java.io.IOException;
import java.net.URL;
import java.util.*;
import javafx.animation.AnimationTimer;
import javafx.animation.PauseTransition;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.input.KeyEvent;
import javafx.scene.paint.Color;
import javafx.scene.text.Text;
import javafx.stage.Stage;
import javafx.util.Duration;

import static org.example.Direction.*;

public class GameController {
    @FXML
    private ResourceBundle resources;
    @FXML
    private URL location;
    @FXML
    private Canvas game;
    @FXML
    private Text text;
    private GraphicsContext gc;

    AnimationTimer timer;
    private long lastUpdate;

    private int score;
    private Direction currentDir, nextDir;
    private final int SIZE = 40;

    private final List<List<Integer>> coordinates = new ArrayList<>();
    private List<List<Integer>> lastPositionCoordinates;

    private final Random random = new Random();
    private List<Integer> foodCoordinates = new ArrayList<>();
    private boolean isFood = false;

    private boolean isGame;
    private boolean isSwitching = false;
    @FXML
    void setDirection(KeyEvent event) {
        Direction newDir = switch (event.getCode()) {
            case W -> UP;
            case S -> DOWN;
            case A -> LEFT;
            case D -> RIGHT;
            default -> null;
        };

        if (newDir != null && !newDir.isOpposite(currentDir)) {
            nextDir = newDir;
        }
    }


    @FXML
    void initialize() {
        gc = game.getGraphicsContext2D();
        renderGameField();
        startGame();
    }

    private void startGame() {
        resetGameSettings();
        timer = new AnimationTimer() {
            @Override
            public void handle(long now) {
                if (now - lastUpdate >= 150_000_000L) {
                    update();
                    if (isGame) {
                        renderGameField();
                        renderSnakeAndFood();
                    }
                    lastUpdate = now;
                }
            }
        };
        timer.start();
    }
    private void stopGame() {
        timer.stop();
        renderGameField();
        renderLastPos();
        switchScene();
    }

    private void resetGameSettings() {
        score = 0;
        isGame = true;
        lastUpdate = 0;
        coordinates.clear();
        coordinates.add(List.of(5,7));
        coordinates.add(List.of(6,7));
        coordinates.add(List.of(7,7));
        spawnFood();
        currentDir = RIGHT;
        nextDir = RIGHT;
    }

    private boolean checkGame() {
        return !(checkGameField() || checkCollision());
    }
    private boolean checkGameField() {
        int MAX_CORD = 14;
        int x = coordinates.getLast().getFirst();
        int y = coordinates.getLast().getLast();
        return (x < 0 || y < 0 || x > MAX_CORD || y > MAX_CORD);
    }
    private boolean checkCollision() {
        List<Integer> head = coordinates.getLast();
        return coordinates.subList(0, coordinates.size() - 1).contains(head);
    }

    private void doStep() {
        switch (currentDir) {
            case UP -> move(0, -1);
            case DOWN -> move(0, 1);
            case LEFT -> move(-1, 0);
            case RIGHT -> move(1, 0);
        }

    }
    private void move(int dx, int dy) {
        lastPositionCoordinates = new ArrayList<>(coordinates);
        int x = coordinates.getLast().getFirst();
        int y = coordinates.getLast().getLast();
        coordinates.add(List.of(x + dx, y + dy));
        if (!isFood) {
            coordinates.removeFirst();
        } else {
            score++;
            text.setText("Score: " + score);
        }
    }


    private void spawnFood() {
        int x = random.nextInt(15);
        int y = random.nextInt(15);
        while (coordinates.contains(List.of(x, y))) {
            x = random.nextInt(15);
            y = random.nextInt(15);
        }
        foodCoordinates = List.of(x, y);
    }
    private boolean checkFood() {
        int x = foodCoordinates.getFirst();
        int y = foodCoordinates.getLast();
        return (x == coordinates.getLast().getFirst() && y == coordinates.getLast().getLast());
    }


    private void update() {
        currentDir = nextDir;
        isGame = checkGame();
        if (!checkGame()) {
            stopGame();
        } else {
            isFood = checkFood();
            if (isFood) {
                spawnFood();
            }
            doStep();
        }
    }

    private void renderLastPos() {
        gc.setFill(Color.RED);
        gc.fillRect(foodCoordinates.getFirst()*SIZE, foodCoordinates.getLast()*SIZE, SIZE, SIZE);
        gc.setFill(Color.DARKBLUE);
        for (List<Integer> cord : lastPositionCoordinates) {
            gc.fillRect(cord.getFirst()*SIZE, cord.getLast()*SIZE, SIZE, SIZE);
        }
    }
    private void renderSnakeAndFood() {
        gc.setFill(Color.RED);
        gc.fillRect(foodCoordinates.getFirst()*SIZE, foodCoordinates.getLast()*SIZE, SIZE, SIZE);
        gc.setFill(Color.DARKBLUE);
        for (List<Integer> cord : coordinates) {
            gc.fillRect(cord.getFirst()*SIZE, cord.getLast()*SIZE, SIZE, SIZE);
        }
    }
    private void renderGameField() {
        gc.setFill(Color.SEAGREEN);
        gc.fillRect(0, 0, 600, 600);
        gc.setFill(Color.DARKSEAGREEN);
        for (int x = 0; x < 15; x++) {
            for (int y = 0; y < 15; y++) {
                if (x % 2 == 0 && y % 2 == 1 || x % 2 == 1 && y % 2 == 0) {
                    gc.fillRect(x*SIZE, y*SIZE, SIZE, SIZE);
                }
            }
        }
    }


    private void switchScene() {
        if (isSwitching) return;
        isSwitching = true;

        PauseTransition pause = new PauseTransition(Duration.seconds(1));

        pause.setOnFinished(event -> {
            try {
                FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("/game-over.fxml"));
                Parent root = fxmlLoader.load();

                GameOverController controller = fxmlLoader.getController();
                controller.setFinalScore(score);

                Stage stage = (Stage) game.getScene().getWindow();
                stage.setScene(new Scene(root));
            } catch (IOException _) {
            }
        });
        pause.play();
    }
}

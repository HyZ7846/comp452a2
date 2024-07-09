package assign2.assign2;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Rectangle;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.util.*;

public class GameApplication extends Application {
    private static final int GRID_SIZE = 16;
    private Rectangle[][] cells = new Rectangle[GRID_SIZE][GRID_SIZE];
    private Circle robot;

    private enum Tool { NONE, START_POINT, END_POINT, GRASSLAND, SWAMPLAND, OBSTACLE }
    private Tool selectedTool = Tool.NONE;

    private Rectangle startPoint = null;
    private Rectangle endPoint = null;
    private Button playButton;

    @Override
    public void start(Stage stage) {
        Label welcomeText = new Label("Trez's Portable Path-Finding Game");

        Button setStartPointButton = new Button("Start Point");
        setStartPointButton.setTextFill(Color.BLUE);
        setStartPointButton.setOnAction(e -> selectedTool = Tool.START_POINT);

        Button setEndPointButton = new Button("End Point");
        setEndPointButton.setTextFill(Color.RED);
        setEndPointButton.setOnAction(e -> selectedTool = Tool.END_POINT);

        Button setGrasslandButton = new Button("Grassland");
        setGrasslandButton.setTextFill(Color.GREEN);
        setGrasslandButton.setOnAction(e -> selectedTool = Tool.GRASSLAND);

        Button setSwamplandButton = new Button("Swampland");
        setSwamplandButton.setTextFill(Color.BROWN);
        setSwamplandButton.setOnAction(e -> selectedTool = Tool.SWAMPLAND);

        Button setObstacleButton = new Button("Obstacle");
        setObstacleButton.setStyle("-fx-font-weight: bold; -fx-text-fill: black;");
        setObstacleButton.setOnAction(e -> selectedTool = Tool.OBSTACLE);

        playButton = new Button("PLAY");
        playButton.setVisible(false);
        playButton.setOnAction(e -> playGame());

        HBox toolBox = new HBox(10, setStartPointButton, setEndPointButton, setGrasslandButton, setSwamplandButton, setObstacleButton);
        toolBox.setAlignment(javafx.geometry.Pos.CENTER);

        GridPane gameGrid = new GridPane();
        gameGrid.setHgap(3);
        gameGrid.setVgap(3);

        for (int row = 0; row < GRID_SIZE; row++) {
            for (int col = 0; col < GRID_SIZE; col++) {
                Rectangle cell = new Rectangle();
                cell.setFill(Color.LIGHTGRAY);
                cell.setStroke(Color.BLACK);
                cell.setOnMouseClicked(this::handleCellClick);
                cells[row][col] = cell;
                gameGrid.add(cell, col, row);
            }
        }

        VBox root = new VBox(20, welcomeText, toolBox, playButton, gameGrid);
        root.setAlignment(javafx.geometry.Pos.CENTER);
        root.setPadding(new javafx.geometry.Insets(30));  // Add 30-pixel margin

        Scene scene = new Scene(root, 560, 660);  // Adjusted size to account for the 30-pixel margin
        stage.setTitle("Path-Finding Game");
        stage.setScene(scene);
        stage.setResizable(false);  // Make the window non-resizable
        stage.show();

        resizeCells(gameGrid, stage);
    }

    private void resizeCells(GridPane gridPane, Stage stage) {
        double availableWidth = stage.getWidth() - 90;  // Subtract some padding/margin
        double availableHeight = stage.getHeight() - 170; // Subtract some padding/margin and space taken by other components
        double cellSize = Math.min(availableWidth / GRID_SIZE, availableHeight / GRID_SIZE);

        for (int row = 0; row < GRID_SIZE; row++) {
            for (int col = 0; col < GRID_SIZE; col++) {
                Rectangle cell = cells[row][col];
                cell.setWidth(cellSize);
                cell.setHeight(cellSize);
            }
        }
    }

    private void handleCellClick(MouseEvent event) {
        Rectangle clickedCell = (Rectangle) event.getSource();
        int row = GridPane.getRowIndex(clickedCell);
        int col = GridPane.getColumnIndex(clickedCell);

        // If the cell is already filled with a color, reset it to light gray
        if (!clickedCell.getFill().equals(Color.LIGHTGRAY)) {
            if (clickedCell.equals(startPoint)) {
                startPoint = null;
            } else if (clickedCell.equals(endPoint)) {
                endPoint = null;
            }
            clickedCell.setFill(Color.LIGHTGRAY);
            updatePlayButtonVisibility();
            return;
        }

        // Otherwise, set the cell based on the selected tool
        switch (selectedTool) {
            case START_POINT:
                if (startPoint != null) {
                    startPoint.setFill(Color.LIGHTGRAY);
                }
                clickedCell.setFill(Color.BLUE);
                startPoint = clickedCell;
                selectedTool = Tool.NONE;
                break;
            case END_POINT:
                if (endPoint != null) {
                    endPoint.setFill(Color.LIGHTGRAY);
                }
                clickedCell.setFill(Color.RED);
                endPoint = clickedCell;
                selectedTool = Tool.NONE;
                break;
            case GRASSLAND:
                clickedCell.setFill(Color.GREEN);
                selectedTool = Tool.NONE;
                break;
            case SWAMPLAND:
                clickedCell.setFill(Color.BROWN);
                selectedTool = Tool.NONE;
                break;
            case OBSTACLE:
                clickedCell.setFill(Color.BLACK);
                selectedTool = Tool.NONE;
                break;
            default:
                break;
        }
        updatePlayButtonVisibility();
    }

    private void updatePlayButtonVisibility() {
        playButton.setVisible(startPoint != null && endPoint != null);
    }

    private void playGame() {
        // Initialize the robot at the start point
        if (robot != null) {
            ((GridPane) robot.getParent()).getChildren().remove(robot);
        }

        double cellSize = cells[0][0].getWidth();
        robot = new Circle(cellSize / 2);
        robot.setFill(Color.YELLOW);
        GridPane.setRowIndex(robot, GridPane.getRowIndex(startPoint));
        GridPane.setColumnIndex(robot, GridPane.getColumnIndex(startPoint));

        GridPane gameGrid = (GridPane) startPoint.getParent();
        gameGrid.getChildren().add(robot);

        // Find the path using A* algorithm
        List<int[]> path = findPath(GridPane.getRowIndex(startPoint), GridPane.getColumnIndex(startPoint),
                GridPane.getRowIndex(endPoint), GridPane.getColumnIndex(endPoint));

        // Animate the robot movement
        animatePath(path);
    }

    private List<int[]> findPath(int startRow, int startCol, int endRow, int endCol) {
        // A* algorithm implementation
        PriorityQueue<Node> openList = new PriorityQueue<>(Comparator.comparingDouble(n -> n.f));
        Map<String, Node> allNodes = new HashMap<>();

        Node startNode = new Node(startRow, startCol, null, 0, estimateCost(startRow, startCol, endRow, endCol));
        openList.add(startNode);
        allNodes.put(startRow + "," + startCol, startNode);

        while (!openList.isEmpty()) {
            Node current = openList.poll();
            if (current.row == endRow && current.col == endCol) {
                return constructPath(current);
            }

            for (int[] dir : new int[][]{{0, 1}, {1, 0}, {0, -1}, {-1, 0}}) {
                int newRow = current.row + dir[0];
                int newCol = current.col + dir[1];
                if (newRow < 0 || newRow >= GRID_SIZE || newCol < 0 || newCol >= GRID_SIZE) continue;

                double newCost = current.g + getCost(newRow, newCol);
                Node neighbor = allNodes.get(newRow + "," + newCol);
                if (neighbor == null) {
                    neighbor = new Node(newRow, newCol, current, newCost, newCost + estimateCost(newRow, newCol, endRow, endCol));
                    openList.add(neighbor);
                    allNodes.put(newRow + "," + newCol, neighbor);
                } else if (newCost < neighbor.g) {
                    neighbor.g = newCost;
                    neighbor.f = newCost + neighbor.h;
                    neighbor.parent = current;
                    openList.add(neighbor);
                }
            }
        }
        return Collections.emptyList();  // No path found
    }

    private List<int[]> constructPath(Node node) {
        List<int[]> path = new ArrayList<>();
        while (node != null) {
            path.add(new int[]{node.row, node.col});
            node = node.parent;
        }
        Collections.reverse(path);
        return path;
    }

    private double getCost(int row, int col) {
        Color color = (Color) cells[row][col].getFill();
        if (color.equals(Color.LIGHTGRAY)) return 1;
        if (color.equals(Color.GREEN)) return 3;
        if (color.equals(Color.BROWN)) return 4;
        return Double.MAX_VALUE;  // Obstacle
    }

    private double estimateCost(int row1, int col1, int row2, int col2) {
        return Math.abs(row1 - row2) + Math.abs(col1 - col2);  // Manhattan distance
    }

    private void animatePath(List<int[]> path) {
        if (path.isEmpty()) {
            System.out.println("No path found.");
            return;
        }

        Timeline timeline = new Timeline();
        for (int i = 0; i < path.size(); i++) {
            int[] point = path.get(i);
            KeyFrame keyFrame = new KeyFrame(Duration.seconds(i * 0.5), e -> {
                GridPane.setRowIndex(robot, point[0]);
                GridPane.setColumnIndex(robot, point[1]);

                // Add small black circle to show the path
                Circle pathCircle = new Circle(robot.getRadius() / 3, Color.BLACK);
                pathCircle.setTranslateX(robot.getRadius() - pathCircle.getRadius());
                pathCircle.setTranslateY(robot.getRadius() - pathCircle.getRadius());
                GridPane.setRowIndex(pathCircle, point[0]);
                GridPane.setColumnIndex(pathCircle, point[1]);
                ((GridPane) robot.getParent()).getChildren().add(pathCircle);
            });
            timeline.getKeyFrames().add(keyFrame);
        }
        timeline.play();
    }

    public static void main(String[] args) {
        launch();
    }

    private static class Node {
        int row, col;
        Node parent;
        double g, h, f;

        Node(int row, int col, Node parent, double g, double f) {
            this.row = row;
            this.col = col;
            this.parent = parent;
            this.g = g;
            this.h = f - g;
            this.f = f;
        }
    }
}

import javafx.application.Application;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.scene.effect.ColorAdjust;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.shape.Line;
import javafx.scene.text.Font;
import javafx.stage.Stage;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Run extends Application {
    // TODO: 17/01/2020 Add castling and pawn swap and en passant
    private ArrayList<double[]> moves = new ArrayList<>();
    private ArrayList<double[]> tempMoves = new ArrayList<>();
    private boolean p1;
    private boolean movePiece = false;
    private boolean pickPiece = true;
    private ImageView[] pieces = new ImageView[32];
    private double x;
    private double y;
    private String id;
    private Label playerTurn;
    private boolean check = false;
    private boolean notInCheck = true;
    private Stage stage;


    public static void main(String[] args) {
        launch();
    }

    public void start(Stage stage) throws Exception {
        p1 = true;
        playerTurn = new Label("White Players turn");
        playerTurn.setFont(new Font(50));
        playerTurn.setTranslateX(210);
        playerTurn.setTranslateY(825);

        Group board = createBoard();
        ImageView[] imageViews = addPieceIcons();

        Line border = getBorder();
        board.getChildren().addAll(imageViews);

        Group window = new Group();
        window.getChildren().addAll(board, border, playerTurn);
        Scene scene = new Scene(window, 790, 900);
        stage.setResizable(false);
        stage.setScene(scene);
        stage.show();
        this.stage = stage;
    }

    private Line getBorder() {
        Line border = new Line();
        border.setStartX(0);
        border.setStartY(800);
        border.setEndX(800);
        border.setEndY(800);
        border.setStrokeWidth(5);
        return border;
    }

    private ImageView[] addPieceIcons() throws FileNotFoundException {
        FileInputStream[] images = new FileInputStream[32];
        File[] folder = {new File("Piece Icons\\Black"), new File("Piece Icons\\White")};
        File[][] files = {folder[0].listFiles(), folder[1].listFiles()};

        for (int i = 0; i < files[0].length + files[1].length; i++) {
            if (i < 16) {
                images[i] = new FileInputStream("Piece Icons\\Black\\" + files[0][i].getName());
            } else {
                images[i] = new FileInputStream("Piece Icons\\White\\" + files[1][i - 16].getName());
            }
        }

        int x = 0;
        int y = 0;
        boolean whitePlayer = true;
        for (int i = 0; i < pieces.length; i++) {
            pieces[i] = new ImageView(new Image(images[i]));
            pieces[i].setMouseTransparent(true);
            pieces[i].setFitHeight(95);
            pieces[i].setFitWidth(95);
            pieces[i].setX(x);
            pieces[i].setY(y);

            if (i < 16) {
                pieces[i].setId(findPattern(files[0][i].getName(), "\\d_[A-Z][a-z]+_[a-zA-Z]+_\\d", 0, 0));
            } else {
                pieces[i].setId(findPattern(files[1][i - 16].getName(), "\\d_[A-Z][a-z]+_[a-zA-Z]+_\\d", 0, 0));
            }

            x += 100;
            if ((i + 1) % 8 == 0 && i > 8 && whitePlayer) {
                y = 600;
                x = 0;
                whitePlayer = false;
            } else if ((i + 1) % 8 == 0) {
                y += 100;
                x = 0;
            }
        }
        return pieces;
    }

    private Group createBoard() throws Exception {
        Group temp = new Group();
        int x = 0;
        int y = 0;
        boolean whiteTile = true;
        ImageView[][] board = new ImageView[8][8];

        for (int row = 0; row < board.length; row++) {
            for (int col = 0; col < board[row].length; col++) {
                if (whiteTile) {
                    board[row][col] = new ImageView(new Image(new FileInputStream("Tiles\\White.png")));
                    whiteTile = false;
                } else {
                    board[row][col] = new ImageView(new Image(new FileInputStream("Tiles\\Grey.png")));
                    whiteTile = true;
                }
                board[row][col].setX(x);
                board[row][col].setY(y);
                board[row][col].setId(getDescription(col, row));
                int finalRow = row;
                int finalCol = col;
                board[row][col].setOnMouseClicked(event -> {
                    if (p1) {
                        playTurn(board, finalRow, finalCol, "White");
                    } else {
                        playTurn(board, finalRow, finalCol, "Black");
                    }
                });
                temp.getChildren().add(board[row][col]);
                y += 100;
            }
            whiteTile = row % 2 != 0;
            x += 100;
            y = 0;
        }
        return temp;
    }

    private void playTurn(ImageView[][] board, int row, int col, String colour) {
        highlightMoves(board, true, colour);
        if (board[row][col].getId().contains(colour) || movePiece) {
            if (pickPiece) {
                pickPiece(board, colour, board[row][col]);
            } else if (movePiece) {
                double newX = board[row][col].getX();
                double newY = board[row][col].getY();
                String newId = board[row][col].getId();
                String opponentColour = getOpponentColour(colour);
                boolean pawn = id.contains("Pawn") && newId.contains("Pawn");

                if ((newId.equalsIgnoreCase("empty") || newId.contains(opponentColour)) && validMove(newX, newY) && !pawn) {
                    movePiece(board, board[row][col], colour, newX, newY, newId.contains(opponentColour));
                } else if (validMove(newX, newY)) {
                    movePiece(board, board[row][col], colour, newX, newY, newId.contains(opponentColour));
                } else if (newId.contains(colour)) {
                    pickPiece(board, colour, board[row][col]);
                }
            }
        }
    }

    private void pickPiece(ImageView[][] board, String colour, ImageView piece) {
        moves.clear();
        addPieceMoves(piece, colour, board, true);
        highlightMoves(board, false, colour);
    }

    private void searchForCheckMate(ImageView[][] board, String colour) {
        moves.clear();
        for (int i = 0; i < 8; i++) {
            for (int j = 0; j < 8; j++) {
                if (board[j][i].getId().contains(colour)) {
                    addPieceMoves(board[j][i], colour, board, true);
                }
            }
        }
        if (moves.size() == 0) {
            //reset
            String player;
            if (p1) {
                player = "Black";
            } else {
                player = "White";
            }

            Alert alert = new Alert(Alert.AlertType.CONFIRMATION, player + " Won");
            alert.showAndWait();

            try {
                stage.close();
                start(new Stage());
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        moves.clear();
    }

    private void highlightMoves(ImageView[][] board, boolean remove, String colour) {
        ColorAdjust colorAdjust = new ColorAdjust();
        colorAdjust.setContrast(0.4);
        colorAdjust.setHue(-0.05);
        colorAdjust.setBrightness(0.5);
        colorAdjust.setSaturation(0.5);
        for (double[] move : moves) {
            highlight(board, remove, colour, colorAdjust, move);
        }
    }

    private void highlight(ImageView[][] board, boolean remove, String colour, ColorAdjust colorAdjust, double[] move) {
        if (remove) {
            board[(int) move[0] / 100][(int) move[1] / 100].setEffect(null);
        } else if (!board[(int) move[0] / 100][(int) move[1] / 100].getId().contains(colour)) {
            board[(int) move[0] / 100][(int) move[1] / 100].setEffect(colorAdjust);
        }
    }

    private void movePiece(ImageView[][] board, ImageView clickedSquare, String colour, double newX, double newY, boolean opponent) {
        int index = findPiece(id);//get the index of the piece clicked
        pieces[index].setX(newX);//move it to the new coordinates
        pieces[index].setY(newY);
        if (opponent) {
            int i = findPiece(clickedSquare.getId());
            pieces[i].imageProperty().setValue(null); //removes piece from the board
        }
        clickedSquare.setId(pieces[index].getId());
        board[(int) x / 100][(int) y / 100].setId("empty");
        p1 = colour.equalsIgnoreCase("black");
        pickPiece = true;
        movePiece = false;
        searchForCheckMate(board, getOpponentColour(colour));
        if (p1) {
            playerTurn.setText("White Players Turn");
        } else {
            playerTurn.setText("Black Players Turn");
        }
    }

    private void addPieceMoves(ImageView piece, String colour, ImageView[][] board, boolean actualMove) {
        String id = piece.getId();
        double x = piece.getX();
        double y = piece.getY();
        if (actualMove) {
            this.x = piece.getX();
            this.y = piece.getY();
            this.id = piece.getId();
        }
        switch (findPattern(id, "(k|e)_[A-Z][a-z]*_", 2, 1)) {
            case "Pawn":
                pawnMoves(x, y, board, colour, piece, actualMove);
                break;
            case "Rook":
                rookMoves(x, y, board, colour, piece, actualMove);
                break;
            case "Knight":
                knightMoves(board, getOpponentColour(colour), x, y, piece, actualMove);
                break;
            case "Bishop":
                bishopMoves(x, y, board, colour, piece, actualMove);
                break;
            case "Queen":
                rookMoves(x, y, board, colour, piece, actualMove);
                bishopMoves(x, y, board, colour, piece, actualMove);
                break;
            case "King":
                kingMoves(x, y, board, getOpponentColour(colour), piece, actualMove);
                break;
            default:
                System.out.println("Error piece not recognised");
                break;
        }
        pickPiece = false;
        movePiece = true;
    }

    private void kingMoves(double x, double y, ImageView[][] board, String opponentColour, ImageView piece, boolean actualMove) {
        double[] newX = new double[]{x + 100, x - 100, x + 100, x - 100, x + 100, x - 100, x, x};
        double[] newY = new double[]{y + 100, y + 100, y - 100, y - 100, y, y, y + 100, y - 100};

        for (int i = 0; i < 8; i++) {
            addConsecutiveMoves(newX[i], newY[i], board, getOpponentColour(opponentColour), piece, actualMove, 0, 0, true);
        }
    }

    private void bishopMoves(double x, double y, ImageView[][] board, String colour, ImageView piece, boolean actualMove) {
        double yUp = y - 100;
        double yDown = y + 100;
        double xRight = x + 100;
        double xLeft = x - 100;

        addConsecutiveMoves(xRight, yUp, board, colour, piece, actualMove, 100, -100, false);//upper right
        addConsecutiveMoves(xRight, yDown, board, colour, piece, actualMove, 100, 100, false);//bottom right
        addConsecutiveMoves(xLeft, yUp, board, colour, piece, actualMove, -100, -100, false);//upper left
        addConsecutiveMoves(xLeft, yDown, board, colour, piece, actualMove, -100, 100, false);//bottom left
    }

    private void addConsecutiveMoves(double newX, double newY, ImageView[][] board, String colour, ImageView piece, boolean actualMove, int xIncrement, int yIncrement, boolean singleMove) {
        double x = newX;
        double y = newY;
        String opponentColour = getOpponentColour(colour);
        while ((checkBounds(x, y) && isEmpty(x, y, board)) || (checkBounds(x, y) && board[(int) x / 100][(int) y / 100].getId().contains(opponentColour))) {
            addMove(board, opponentColour, x, y, piece, actualMove);
            if (board[(int) x / 100][(int) y / 100].getId().contains(opponentColour)) {
                break;
            }
            if (singleMove) {
                break;
            }
            x += xIncrement;
            y += yIncrement;
        }
    }

    private void rookMoves(double x, double y, ImageView[][] board, String colour, ImageView piece, boolean actualMove) {
        addConsecutiveMoves(x + 100, y, board, colour, piece, actualMove, 100, 0, false);//all right
        addConsecutiveMoves(x - 100, y, board, colour, piece, actualMove, -100, 0, false);//all left
        addConsecutiveMoves(x, y + 100, board, colour, piece, actualMove, 0, 100, false);//all up/down
        addConsecutiveMoves(x, y - 100, board, colour, piece, actualMove, 0, -100, false);//all down/up
    }

    private boolean isEmpty(double x, double y, ImageView[][] board) {
        String id = board[(int) (x / 100)][(int) (y / 100)].getId();
        return id.equalsIgnoreCase("empty");
    }

    private void knightMoves(ImageView[][] board, String opponentColour, double x, double y, ImageView piece, boolean actualMove) {
        double[] newX = new double[]{x + 100, x + 100, x - 100, x - 100, x + 200, x + 200, x - 200, x - 200};
        double[] newY = new double[]{y + 200, y - 200, y + 200, y - 200, y + 100, y - 100, y + 100, y - 100};
        for (int i = 0; i < 8; i++) {
            addConsecutiveMoves(newX[i], newY[i], board, getOpponentColour(opponentColour), piece, actualMove, 0, 0, true);
        }
    }

    private boolean checkBounds(double x, double y) {
        return x >= 0 && x <= 700 && y >= 0 && y <= 700;
    }

    private boolean validMove(double newX, double newY) {
        for (double[] move : moves) {
            if (newX == move[0] && newY == move[1]) {
                return true;
            }
        }
        return false;
    }

    private void pawnMoves(double x, double y, ImageView[][] board, String colour, ImageView piece, boolean actualMove) {
        if (colour.equalsIgnoreCase("White")) {
            addPawnMoves(x, y, board, "Black", y - 100, piece, actualMove);
        } else {
            addPawnMoves(x, y, board, "White", y + 100, piece, actualMove);
        }
    }

    private void addPawnMoves(double x, double y, ImageView[][] board, String opponentColour, double direction, ImageView piece, boolean actualMove) {
        if (opponentColour.equalsIgnoreCase("Black") && y == 600.0 && board[(int) x / 100][(int) (y - 100) / 100].getId().equalsIgnoreCase("empty") && board[(int) x / 100][(int) (y - 200) / 100].getId().equalsIgnoreCase("empty")) {
            addMove(board, opponentColour, x, y - 200, piece, actualMove);//move white up 2 squares
        } else if (opponentColour.equalsIgnoreCase("White") && y == 100 && board[(int) x / 100][(int) (y + 100) / 100].getId().equalsIgnoreCase("empty") && board[(int) x / 100][(int) (y + 200) / 100].getId().equalsIgnoreCase("empty")) {
            addMove(board, opponentColour, x, y + 200, piece, actualMove);//move black down 2 squares
        }
        if (board[(int) x / 100][(int) direction / 100].getId().equalsIgnoreCase("empty")) {
            addMove(board, opponentColour, x, direction, piece, actualMove);//move white/black up/down 1 square
        }
        if ((x + 100) <= 700 && (x + 100) >= 0 && direction >= 0 && direction <= 700) {
            if (board[(int) ((x + 100) / 100)][(int) ((direction) / 100)].getId().contains(opponentColour)) {
                addMove(board, opponentColour, x + 100, direction, piece, actualMove);//look at right diagonal
            }
        }
        if ((x - 100) <= 700 && (x - 100) >= 0 && direction >= 0 && direction <= 700) {
            if (board[(int) ((x - 100) / 100)][(int) ((direction) / 100)].getId().contains(opponentColour)) {
                addMove(board, opponentColour, x - 100, direction, piece, actualMove);//look at left diagonal
            }
        }
    }

    private void addMove(ImageView[][] board, String opponentColour, double x, double y, ImageView piece, boolean actualMove) {
        if (check) {
            if (moveGetsOutOfCheck(board, opponentColour, new double[]{x, y}, piece)) {
                addMove(x, y, actualMove);
            }
        } else {
            if (notInCheck) {//check if the next move will result in check
                if (!moveGetsInCheck(board, opponentColour, new double[]{x, y}, piece)) {
                    addMove(x, y, actualMove);
                }
                notInCheck = true;
            } else {
                addMove(x, y, actualMove);
            }
        }
    }

    private void addMove(double x, double y, boolean actualMove) {
        if (actualMove) {
            moves.add(new double[]{x, y});//look at left diagonal
        } else {
            tempMoves.add(new double[]{x, y});
        }
    }

    private boolean moveGetsInCheck(ImageView[][] board, String opponentColour, double[] move, ImageView piece) {
        int row = (int) piece.getX() / 100;
        int col = (int) piece.getY() / 100;
        int moveX = (int) move[0] / 100;
        int moveY = (int) move[1] / 100;

        String id = board[row][col].getId();
        String moveId = board[moveX][moveY].getId();
        board[row][col].setId("empty");
        board[moveX][moveY].setId(id);
        notInCheck = false;
        if (searchForCheck(opponentColour, board)) {
            board[row][col].setId(id);
            board[moveX][moveY].setId(moveId);
            notInCheck = false;
            return true;
        }
        board[row][col].setId(id);
        board[moveX][moveY].setId(moveId);
        notInCheck = true;
        return false;
    }


    private boolean moveGetsOutOfCheck(ImageView[][] board, String opponentColour, double[] move, ImageView piece) {
        int moveX = (int) move[0] / 100;
        int moveY = (int) move[1] / 100;
        int row = (int) piece.getX() / 100;
        int col = (int) piece.getY() / 100;

        String id = board[row][col].getId();
        String moveId = board[moveX][moveY].getId();
        board[row][col].setId("empty");
        board[moveX][moveY].setId(id);

        check = false;
        if (searchForCheck(opponentColour, board)) {
            check = true;
        } else {
            board[row][col].setId(id);
            board[moveX][moveY].setId(moveId);
            check = true;
            return true;
        }

        board[row][col].setId(id);
        board[moveX][moveY].setId(moveId);
        return false;
    }


    private boolean searchForCheck(String colour, ImageView[][] board) {
        tempMoves.clear();
        for (int i = 0; i < 8; i++) {
            for (int j = 0; j < 8; j++) {
                if (board[j][i].getId().contains(colour)) {
                    addPieceMoves(pieces[findPiece(board[j][i].getId())], colour, board, false);
                }
            }
        }

        for (double[] move : tempMoves) {
            if (board[(int) move[0] / 100][(int) move[1] / 100].getId().contains(getOpponentColour(colour) + "_King")) {
                return true;
            }
        }
        return false;
    }

    private int findPiece(String id) {
        for (int i = 0; i < pieces.length; i++) {
            if (pieces[i].getId().equalsIgnoreCase(id)) {
                return i;
            }
        }
        return 0;
    }

    private String getDescription(int row, int col) {
        if ((row == 0 || row == 7) && (col == 0 || col == 7)) {
            return row + getColour(row) + "_Rook_" + col;
        } else if ((row == 0 || row == 7) && (col == 1 || col == 6)) {
            return row + getColour(row) + "_Knight_" + col;
        } else if ((row == 0 || row == 7) && (col == 2 || col == 5)) {
            return row + getColour(row) + "_Bishop_" + col;
        } else if ((row == 0 || row == 7) && (col == 3)) {
            return row + getColour(row) + "_Queen_" + col;
        } else if ((row == 0 || row == 7) && (col == 4)) {
            return row + getColour(row) + "_King_" + col;
        } else if ((row == 1 || row == 6)) {
            return row + getColour(row) + "_Pawn_" + col;
        }
        return "empty";
    }

    private String getColour(int row) {
        return (row == 0 || row == 1) ? "_Black" : "_White";
    }

    private String findPattern(String string, String regex, int start, int end) {
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(string);
        while (matcher.find()) {
            string = string.substring(matcher.start() + start, matcher.end() - end);
        }
        return string;
    }

    private String getOpponentColour(String colour) {
        if (colour.equalsIgnoreCase("white")) {
            return "Black";
        } else {
            return "White";
        }
    }
}

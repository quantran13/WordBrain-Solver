package personal.wordbrainsolver;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.AnchorPane;

public class FXMLController implements Initializable {

    private HashMap<String, Integer> dictionary;
    private final HashMap<String, Integer> wordsResult;
    private int wordLength;

    private final ArrayList<ArrayList<TextField>> textFieldGrid;

    @FXML
    private AnchorPane inputAnchorPane;
    @FXML
    private TextArea txtOutputWords;
    @FXML
    private TextField txtWordLength;
    @FXML
    private ComboBox<Integer> cboGridWidth;
    @FXML
    private ComboBox<Integer> cboGridHeight;

    public FXMLController() {
        this.wordLength = 0;
        this.dictionary = new HashMap<>();
        this.wordsResult = new HashMap<>();

        this.textFieldGrid = new ArrayList<>();
    }

    @Override
    public void initialize(URL url, ResourceBundle rb) {

        /*
         * Read the dictionary and store in a hashmap
         */
        String dictionaryFile = "dictionary.txt";

        try {
            dictionary = readDictionary(dictionaryFile);
        } catch (IOException ex) {
            String title = "Error reading the dictionary";
            String message;

            if (ex instanceof FileNotFoundException) {
                message = "File dictionary not found!";
            } else {
                message = "An error happen while reading from the dictionary!";
            }

            showErrorMessage(title, message, AlertType.ERROR, true);
        } catch (URISyntaxException ex) {
            Logger.getLogger(FXMLController.class.getName()).log(Level.SEVERE, null, ex);
        }

        /*
         * Populate the grid width and height combo boxes
         */
        cboGridWidth.getItems().addAll(1, 2, 3, 4, 5, 6, 7);
        cboGridHeight.getItems().addAll(1, 2, 3, 4, 5, 6, 7);
    }

    /**
     * Check if both combo boxes for the grid's width and height are selected. If yes, enable the characters input area for user to input.
     *
     * @param event
     */
    @FXML
    protected void cboGridWidthHeight_Action(ActionEvent event) {
        boolean isWidthComboBoxEmpty = cboGridWidth.getSelectionModel().isEmpty();
        boolean isHeightComboBoxEmpty = cboGridHeight.getSelectionModel().isEmpty();

        if (!isWidthComboBoxEmpty && !isHeightComboBoxEmpty) {
            /*
             * Get the width and height.
             */
            int width = cboGridWidth.getValue();
            int height = cboGridHeight.getValue();

            /*
             * Get the input characters label's position to determine the
             * starting position to draw the input text fields for the grid.
             */
            double startingPosY = 0;
            double startingPosX = 0;

            double currentX = startingPosX;
            double currentY = startingPosY;

            /*
             * Create text fields to input puzzle information.
             */
            textFieldGrid.clear();
            inputAnchorPane.getChildren().clear();

            // Filter out characters that are not letters nor blank space,
            // which is denoted by a dot, or prevent characters from being 
            // inputted if the text field has already been filled.
            EventHandler letterFilter = new EventHandler<KeyEvent>() {
                @Override
                public void handle(KeyEvent event) {
                    String text = event.getCharacter();

                    if (text.length() > 0) {
                        char c = text.charAt(0);
                        boolean isLowercase = (c >= 'a' && c <= 'z');
                        boolean isUppercase = (c >= 'A' && c <= 'Z');
                        boolean isLetter = isUppercase || isLowercase;

                        boolean isBlank = (c == '.');

                        TextField sourceField = (TextField) event.getSource();
                        boolean hasExceeded = (sourceField.getText().length() != 0);

                        if ((!isLetter && !isBlank) || hasExceeded) {
                            event.consume();
                        }
                    } else {
                        event.consume();
                    }
                }
            };

            for (int i = 0; i < height; i++) {
                ArrayList<TextField> row = new ArrayList<>();

                for (int k = 0; k < width; k++) {
                    TextField newTextField = new TextField();
                    newTextField.setLayoutX(currentX);
                    newTextField.setLayoutY(currentY);
                    newTextField.setMaxSize(30, 20);

                    newTextField.addEventFilter(KeyEvent.KEY_TYPED, letterFilter);

                    row.add(newTextField);
                    inputAnchorPane.getChildren().add(row.get(k));

                    currentX += 50;
                }

                textFieldGrid.add(row);
                currentY += 40;
                currentX = startingPosX;
            }
        }
    }

    /**
     * Handle the solve button click event.
     *
     * @param event The button click event.
     */
    @FXML
    protected void btnSolve_Click(ActionEvent event) {
        txtOutputWords.clear();
        wordLength = 0;
        wordsResult.clear();

        /*
         * Get the puzzle dimensions
         */
        int height = 0;
        int width = 0;

        try {
            // Try to get the width/length and convert to integers
            width = cboGridWidth.getValue();
            height = cboGridHeight.getValue();
        } catch (NumberFormatException ex) {
            // If the user hasn't chosen the width/length.
            showErrorMessage("Invalid width/height", "You didn't provide the "
                + "puzzle's width/length!", AlertType.ERROR, false);
        }

        /*
         * Get the possible word length
         */
        String wordLengthText = txtWordLength.getText();

        // Error messages in case an invalid word length is entered
        String title = "Word length input error";
        String message = "Please input a valid word length in the form "
            + "of an unsigned integer!";

        // Get the word length or show an error message if the word length is
        // invalid
        try {
            wordLength = Integer.parseInt(wordLengthText);
        } catch (NumberFormatException ex) {
            showErrorMessage(title, message, AlertType.ERROR, false);
            return;
        }

        if (wordLength <= 0) {
            showErrorMessage(title, message, AlertType.ERROR, false);
            return;
        }

        /*
         * Get the grid of characters.
         */
        char chars[][] = new char[height][width];
        boolean[][] check = new boolean[height][width];
        boolean invalid = false;

        for (int i = 0; i < height; i++) {
            for (int k = 0; k < width; k++) {
                String text = textFieldGrid.get(i).get(k).getText();

                if (text.length() == 1) {
                    char c = text.charAt(0);
                    chars[i][k] = c;
                    check[i][k] = true;
                } else {
                    showErrorMessage("Incomplete input", "You have not entered"
                        + " all the characters!", AlertType.ERROR, false);
                    invalid = true;
                    break;
                }
            }

            if (invalid) {
                break;
            }
        }

        // Solve
        String output = Solve(chars, check, width, height);

        // Write to output area
        txtOutputWords.setText(output);
    }

    /**
     * Solve the given input text and output words that are formed using 
     * the given characters.
     *
     * @param chars Grid of characters.
     * @param width The grid's width.
     * @param height The grid's height.
     *
     * @return String The string containing all the words formed using 
     * the given characters, separating by newline characters.
     */
    private String Solve(char[][] chars, boolean[][] check, int width, int height) {
        String output = "";

        ArrayList<Character> result = new ArrayList<>();

        for (int i = 0; i < height; i++) {
            for (int j = 0; j < width; j++) {
                result.add(chars[i][j]);
                check[i][j] = false;

                permutate(chars, check, result, i, j, height, width);
                result.remove(result.size() - 1);
                check[i][j] = true;
            }
        }

        for (Map.Entry<String, Integer> resultEntry : wordsResult.entrySet()) {
            output += resultEntry.getKey() + "\n";
        }

        return output;
    }

    /**
     * Find all the permutation of the given grid of characters
     *
     * @param chars Grid of characters.
     * @param check A boolean grid to check if the character has been used or not.
     * @param x The current x-coordinate of the position.
     * @param y The current y-coordinate of the position.
     * @param height The grid's height.
     * @param width The grid's width.
     * @param result An ArrayList of strings from which to form the result word.
     */
    private void permutate(char[][] chars, boolean[][] check,
        ArrayList<Character> result, int x, int y, int height, int width) {
        if (result.size() == wordLength) {
            String formedWord = formWord(result);

            if (dictionary.containsKey(formedWord.toUpperCase())) {
                wordsResult.put(formedWord, 1);
            }

            return;
        }

        int offX[] = new int[]{1, 0, -1};
        int offY[] = new int[]{1, 0, -1};

        for (int i = 0; i < offX.length; i++) {
            for (int j = 0; j < offY.length; j++) {
                int offsetX = offX[i];
                int offsetY = offY[j];

                if (offsetX != 0 || offsetY != 0) {
                    int newx = x + offsetX;
                    int newy = y + offsetY;

                    if (inGridBound(newx, newy, height, width)) {
                        if (check[newx][newy]) {
                            check[newx][newy] = false;
                            result.add(chars[newx][newy]);

                            permutate(chars, check, result, newx, newy, 
                                height, width);
                            check[newx][newy] = true;
                            result.remove(result.size() - 1);
                        }
                    }
                }
            }
        }
    }

    /**
     * Check if the given position is in the grid's bound or not.
     *
     * @param x The x coordinate.
     * @param y The y coordinate.
     * @param height The grid's height.
     * @param width The grid's width.
     *
     * @return True if the given position is in the grid's bound, false otherwise.
     */
    private boolean inGridBound(int x, int y, int height, int width) {
        return (x >= 0 && x < height && y >= 0 && y < width);
    }

    /**
     * Take the given strings and form them into a complete word.
     *
     * @param words ArrayList of characters to form a word.
     * @return The formed Word.
     */
    private String formWord(ArrayList<Character> words) {
        String output = "";
        for (int i = 0; i < words.size(); i++) {
            output += words.get(i);
        }

        return output;
    }

    /**
     * Show a popup window informing user about the error.
     *
     * @param title Popup window's title.
     * @param message Error message.
     * @param type The alert type
     * @param exit True if exit the program after showing the message.
     */
    protected void showErrorMessage(String title, String message,
        AlertType type, boolean exit) {
        Alert alert = new Alert(type);

        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);

        alert.showAndWait();
        if (exit) {
            Platform.exit();
        }
    }

    /**
     * Read from a dictionary text file and put the dictionary into a HashMap.
     *
     * @param fileName The dictionary's file name.
     * @return A HashMap containing the dictionary.
     *
     * @throws FileNotFoundException If the dictionary.txt file is not found.
     * @throws IOException If there is an error while reading the file.
     */
    private HashMap<String, Integer> readDictionary(String fileName)
        throws FileNotFoundException, IOException, URISyntaxException {
        // Open the dictionary file and store information in a hashmap.

        try (InputStream instream = getClass().getClassLoader().
                getResourceAsStream(fileName);
            InputStreamReader infile = new InputStreamReader(instream);
            BufferedReader inBuffer = new BufferedReader(infile)) {
            HashMap<String, Integer> dict = new HashMap<>();

            String line;
            while ((line = inBuffer.readLine()) != null) {
                dict.put(line, 1);
            }

            return dict;
        }
    }
}

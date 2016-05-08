package personal.wordbrainsolver;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;

public class FXMLController implements Initializable {

    private HashMap<String, Integer> dictionary;
    private final HashMap<String, Integer> wordsResult;
    private int wordLength;

    @FXML
    private TextArea txtInputCharacters;
    @FXML
    private TextArea txtOutputWords;
    @FXML
    private TextField txtWordLength;
    @FXML
    private ComboBox cboGridWidth;
    @FXML
    private ComboBox cboGridHeight;

    public FXMLController() {
        this.wordLength = 0;
        this.dictionary = new HashMap<>();
        this.wordsResult = new HashMap<>();
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
        cboGridWidth.getItems().addAll("3", "4", "5", "6", "7");
        cboGridHeight.getItems().addAll("3", "4", "5", "6", "7");
    }

    /**
     * Handle the solve button click event.
     *
     * @param event
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
            width = Integer.parseInt((String) cboGridWidth.getValue());
            height = Integer.parseInt((String) cboGridHeight.getValue());
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
        String chars[][] = new String[height][width];
        boolean[][] check = new boolean[height][width];

        String[] inputText = txtInputCharacters.getText().split("\\W+");
        int counter = 0;

        for (int i = 0; i < height; i++) {
            for (int j = 0; j < width; j++) {
                chars[i][j] = inputText[counter];
                check[i][j] = true;
                counter++;
            }
        }

        // Solve
        String output = Solve(chars, check, width, height);

        // Write to output area
        txtOutputWords.setText(output);
    }

    /**
     * Solve the given input text and output words that are formed using the
     * given characters.
     *
     * @param chars Grid of characters.
     * @param width The grid's width.
     * @param height The grid's height.
     *
     * @return String The string containing all the words formed using the given
     * characters, separating by newline characters.
     */
    private String Solve(String[][] chars, boolean[][] check, int width, int height) {
        String output = "";

        ArrayList<String> result = new ArrayList<>();

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
     * @param chars
     * @param check
     * @param x
     * @param y
     * @param height
     * @param width
     * @param result
     */
    private void permutate(String[][] chars, boolean[][] check,
            ArrayList<String> result, int x, int y, int height, int width) {
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

                            permutate(chars, check, result, newx, newy, height, width);
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
     * @return True if the given position is in the grid's bound, false
     * otherwise.
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
    private String formWord(ArrayList<String> words) {
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
       
        try (InputStream instream = getClass().getClassLoader().getResourceAsStream(fileName);
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

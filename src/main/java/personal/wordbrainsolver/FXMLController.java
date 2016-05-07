package personal.wordbrainsolver;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.ResourceBundle;
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
		try {
			dictionary = readDictionary("dictionary.txt");
		} catch (IOException ex) {
			Alert alert = new Alert(AlertType.ERROR);
			alert.setTitle("Error reading dictionary!");
			alert.setHeaderText(null);
			
			if (ex instanceof FileNotFoundException) {
				alert.setContentText("dictionary.txt not found!");
			} else {	
				alert.setContentText("Cannot read the the dictionary" + 
					" from dictionary.txt! Check the file again.");
			}
			
			alert.showAndWait();
			Platform.exit();
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
		
		// Get the puzzle dimensions
		int width = Integer.parseInt((String) cboGridWidth.getValue());
		int height = Integer.parseInt((String) cboGridHeight.getValue());
		
		// Get the possible word lengths
		String wordLengthText = txtWordLength.getText();
		wordLength = Integer.parseInt(wordLengthText);
		
		// Get the grid of characters
		String chars[][] = new String[height][width];
		boolean[][] check = new boolean[height][width];
		
		String[] inputText = txtInputCharacters.getText().split("\\W+");
		int counter = 0;
		
		for (int i = 0; i < height; i++)
			for (int j = 0; j < width; j++) {
				chars[i][j] = inputText[counter];
				check[i][j] = true;
				counter++;
			}
		
		// Solve
		//String output = Solve(chars, check, width, height);
		
		// Write to output area
		//txtOutputWords.setText(output);
	}
	
	/**
	 * Solve the given input text and output words that are formed using the
	 * given characters.
	 * 
	 * @param chars Grid of characters.
	 * @param width The grid's width.
	 * @param height The grid's height.
	 * 
	 * @return String The string containing all the words formed using the
	 * given characters, separating by newline characters.
	 */
	private String Solve(String[][] chars, boolean[][] check, int width, int height) {
		String output = "";
		
		ArrayList<String> result = new ArrayList<>();
		
		for (int i = 0; i < height; i++)
			for (int j = 0; j < width; j++) {
				result.add(chars[i][j]);
				check[i][j] = false;
				
				permutate(chars, check, result, i, j, height, width);
				result.remove(result.size() - 1);
				check[i][j] = true;
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

			if (dictionary.containsKey(formedWord.toUpperCase()))
				wordsResult.put(formedWord, 1);

			return;
		}
		
		int offX[] = new int[] {1, 0, -1};
		int offY[] = new int[] {1, 0, -1};
		
		for (int i = 0; i < offX.length; i++)
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
	private String formWord(ArrayList<String> words) {
		String output = "";
		for (int i = 0; i < words.size(); i++)
			output += words.get(i);
		
		return output;
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
			throws FileNotFoundException, IOException {
		try (FileReader infile = new FileReader(fileName);
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
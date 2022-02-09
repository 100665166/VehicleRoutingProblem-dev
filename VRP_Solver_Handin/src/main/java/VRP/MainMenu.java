package VRP;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.concurrent.ThreadLocalRandom;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import javax.swing.*;

import java.awt.*;


public class MainMenu {

	public static JFrame frame;
	private final JTextField numVehiclesField;
	private final JTextField numLocationsField;
	private final JButton generate;
	private final JButton fromFile;
	private final JList<File> list;
	static VRP_Test3 solver;

	public static void main(String[] args) {
		MainMenu g = new MainMenu();

		KeyboardFocusManager.getCurrentKeyboardFocusManager().addKeyEventDispatcher(new KeyEventDispatcher() {
			@Override
			public boolean dispatchKeyEvent(KeyEvent e) {
				if (e.getKeyCode() == 27)
					solver.stopSearch();
				//if (e.getKeyCode() == 72)
				//	solver.hideInterfaceData();
				return false;
			}
		});
	}

	public MainMenu() {

		// Create frame (window)
		frame = new JFrame("Vehicle Routing");
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.setSize(500, 300);

		// Create Input Fields
		numLocationsField = new JTextField("", 3);
		numVehiclesField = new JTextField("", 3);

		JLabel numVehiclesLabel = new JLabel("Number of vehicles:");
		JLabel numLocationsLabel = new JLabel("Number of locations:");

		generate = new JButton("Generate");
		fromFile = new JButton("Open File");

		// Create problem file drop down menu
		DefaultListModel<File> fileList = new DefaultListModel<>();
		File problemsFolder = new File("./problems");
		File[] problems = problemsFolder.listFiles();
		for(File problem:problems) {
			if(problem.isFile()) {
				fileList.addElement(problem);
			}
		}
		list = new JList<>(fileList);

		// Panel for user input
		JPanel userValuesPanel = new JPanel();
		userValuesPanel.setLayout(new FlowLayout());
		userValuesPanel.add(numLocationsLabel);
		userValuesPanel.add(numLocationsField);
		userValuesPanel.add(numVehiclesLabel);
		userValuesPanel.add(numVehiclesField);
		userValuesPanel.add(generate);

		// Panel for file list
		JPanel filePanel = new JPanel();
		filePanel.add(list);
		filePanel.add(fromFile);


		// Action handler
		Handler handler = new Handler();
		fromFile.addActionListener(handler);
		generate.addActionListener(handler);

		// Panel container
		JPanel container = new JPanel();
		container.setLayout(new BoxLayout(container, BoxLayout.Y_AXIS));
		container.add(userValuesPanel);
		container.add(filePanel);

		frame.add(container);
		frame.setVisible(true);
	}

	int distance = 100;
	int numVehicles, numLocations;
	int[] vCapacity, lCapacity;

	int[][] coords;

	private class Handler implements ActionListener {
		public void actionPerformed(ActionEvent event) {

			if(event.getSource() == fromFile) {
				readFile();
			}
			else if(event.getSource() == generate) {
				numVehicles = Integer.parseInt(numVehiclesField.getText());
				numLocations = Integer.parseInt(numLocationsField.getText());
				coords = genCoords(numLocations);

				int locationCap = 2;

				int vehicleCap = ((int)Math.ceil(numLocations/numVehicles) + 1) * locationCap;

				if (numVehicles > numLocations)
				{
					vehicleCap = locationCap;

					vCapacity = new int[numLocations];
					for (int i = 0; i < numLocations; ++i) {
						vCapacity[i] = vehicleCap;
					}
				}
				else
				{
					vCapacity = new int[numVehicles];
					for (int i = 0; i < numVehicles; ++i) {
						vCapacity[i] = vehicleCap;
					}
				}

				lCapacity = new int[numLocations];
				for (int i = 0; i < numLocations; ++i) {
					lCapacity[i] = locationCap;
				}

				if (numVehicles > numLocations)
				{
					solver = new VRP_Test3(numLocations + 1, numLocations, coords, distance, lCapacity, vCapacity);
				}
				else
				{
					solver = new VRP_Test3(numLocations + 1, numVehicles, coords, distance, lCapacity, vCapacity);
				}
				//solver = new VRP_Test3(numLocations + 1, numVehicles, coords, distance, lCapacity, vCapacity);
			}
		}
	}


	private void readFile() {
		if(list.getSelectedIndex() != -1) {
			// Launch solver with the selected file

			JSONParser parser = new JSONParser();
			try {
				Object obj = parser.parse(new FileReader(list.getSelectedValue()));
				JSONObject jsonObject = (JSONObject) obj;
				JSONArray jArray;
				int i = 0;

				// Get number of locations
				numLocations = ((Long)jsonObject.get("location")).intValue();

				// Get location capacities
				lCapacity = new int[numLocations - 1];
				jArray = (JSONArray) jsonObject.get("lCapacity");
				for (Object capacity: jArray)
				{
					lCapacity[i] = ((Long)capacity).intValue();
					i++;
				}
				i = 0;

				// Get number of vehicles
				numVehicles = ((Long)jsonObject.get("vehicle")).intValue();

				// Get vehicle capacities
				if (numVehicles > (numLocations-1))
				{
					vCapacity = new int[numLocations-1];
					jArray = (JSONArray) jsonObject.get("vCapacity");
					for (Object capacity: jArray)
					{
						vCapacity[i] = ((Long)capacity).intValue();
						i++;
						if (i >= vCapacity.length)
						{
							break;
						}
					}
				}
				else
				{
					vCapacity = new int[numVehicles];
					jArray = (JSONArray) jsonObject.get("vCapacity");
					for (Object capacity: jArray)
					{
						vCapacity[i] = ((Long)capacity).intValue();
						i++;
					}
				}
				i = 0;

				// Get coords
				coords = new int[numLocations + 1][2];
				jArray = (JSONArray) jsonObject.get("lCoords");
				for (Object coord: jArray)
				{
					int j = 0;
					for (Object subValue: (JSONArray)coord)
					{
						coords[i][j] = ((Long)subValue).intValue();
						j++;
					}
					i++;
				}
				//solver = new VRP_Test3(numLocations, numVehicles, coords, distance, lCapacity, vCapacity);

				if (numVehicles > (numLocations-1))
				{
					solver = new VRP_Test3(numLocations, numLocations-1, coords, distance, lCapacity, vCapacity);
				}
				else
				{
					solver = new VRP_Test3(numLocations, numVehicles, coords, distance, lCapacity, vCapacity);
				}

			} catch (IOException | ParseException e) {
				e.printStackTrace();
			}
		} else {
			System.out.println("Select a file.");
		}
	}

	private int[][] genCoords(int num) {
		int[][] randCoords = new int[num+1][2]; // 2D array to store the randomly generated points
		int[] newPoint = new int[2]; // A point to store a new point before it is added to the randCoords
		boolean duplicate = false;

		randCoords[0][0] = randCoords[0][1] = distance; // Location for depot at center

		for (int i = 0; i < num;) {

			// Generate a random x,y
			newPoint[0] = ThreadLocalRandom.current().nextInt(0, distance*2 + 1);
			newPoint[1] = ThreadLocalRandom.current().nextInt(0, distance*2 + 1);

			// Check if newPoint exists already
			for (int[] b : randCoords)
			{
				if (newPoint[0] == b[0] && newPoint[1] == b[1])
				{
					// Point exists, terminate loop
					duplicate = true;
					break;
				}
			}

			if (!duplicate) {
				//System.out.println(newPoint[0] + " " + newPoint[1]);
				randCoords[i + 1][0] = newPoint[0];
				randCoords[i + 1][1] = newPoint[1];
				i++;
			} else {
				duplicate = false;
			}
		}
		return randCoords;
	}
}

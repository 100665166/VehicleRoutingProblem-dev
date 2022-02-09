package VRP;

import javax.swing.*;
import java.awt.*;
import java.util.Arrays;
import java.util.Random;

public class SolverInterface extends JFrame {

	int SCREEN_WIDTH = 600;
	int SCREEN_HEIGHT = 650;

	int numVehicles;
	int numLocations;
	int[][] coords;
	int distance;
	int _combo;
	int _bestArrangementDist;
	int[] _bestArrangementofVehicles;
	int[] _bestRouteDistance;
	Color[] vehicleColors;
	boolean showData = true;

	myPanel panel;

	int[][] bestArrangement = null;

	public SolverInterface(int _numVehicles, int _numLocations, int[][] _coords, int _distance)
	{
		numVehicles = _numVehicles;
		numLocations = _numLocations;
		coords = _coords;
		distance = _distance;

		vehicleColors = new Color[numVehicles];
		for (int i = 0; i < numVehicles; ++i) {
			vehicleColors[i] = randColor();
		}

		panel = new myPanel();
		panel.setVisible(true);

		this.add(panel);

		this.pack();
		this.setTitle("Vehicle Routing");
		this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		this.setResizable(false);
		this.setVisible(true);

	}

	public void newBestArrangement(int[][] newArrangement, int combo, int bestArrangementDist, int[] bestArrangmentofVehicles, int[] bestRouteDistance)
	{
		bestArrangement = newArrangement;
		_combo = combo;
		_bestArrangementDist = bestArrangementDist;
		_bestArrangementofVehicles = bestArrangmentofVehicles;
		_bestRouteDistance = bestRouteDistance;
		repaint();
	}


	public class myPanel extends JPanel {

		myPanel() {
			this.setPreferredSize(new Dimension(SCREEN_WIDTH , SCREEN_HEIGHT));
		}

		public void paint(Graphics g) {

			Graphics2D g2d = (Graphics2D) g;
			for(var i = 0; i < numLocations; i++)
			{
				// Draw each point
				g2d.drawOval((coords[i][0] * SCREEN_WIDTH) / (distance * 2) - 4, (coords[i][1] * (SCREEN_HEIGHT - 50)) / (distance * 2) - 4, 8, 8);
			}

			// Draw solver data to interface
			g2d.setColor(Color.black);
			g2d.drawString("Press 'ESC' to stop searching.", 225, 625);
			if (showData) {
				g2d.drawString("Combination: " + _combo, 2, 15);
				g2d.drawString("Distance: " + _bestArrangementDist, 2, 30);
			}


			// Draw each connection
			if (bestArrangement != null) {
				int from;
				int to = 0;
				int index = 0;

				for (int[] vehicle : bestArrangement) {
					g2d.setColor(vehicleColors[index]);
					for (int location : vehicle) {
						from = to;
						to = location;

						g2d.drawLine(
								(coords[from][0] * SCREEN_WIDTH) / (distance * 2),
								(coords[from][1] * (SCREEN_HEIGHT - 50)) / (distance * 2),
								(coords[to][0] * SCREEN_WIDTH) / (distance * 2),
								(coords[to][1] * (SCREEN_HEIGHT - 50)) / (distance * 2)
						);
					}
					if (showData)
						g2d.drawString("V" + _bestArrangementofVehicles[index] + ": " + Arrays.toString(vehicle) + " -> Cost: " + _bestRouteDistance[index], 2, 45 + (15 * index));
					index++;
				}
			}

		}

	}
	private Color randColor() {
		Random rand = new Random();
		float r = rand.nextFloat();
		float g = rand.nextFloat();
		float b = rand.nextFloat();
		return new Color(r,g,b);
	}

	public void hideData() {
		showData = !showData;
		repaint();
	}

}


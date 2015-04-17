package tests;

import java.util.Scanner;
import java.util.ArrayList;
import java.io.PrintWriter;
import java.io.FileNotFoundException;
import java.io.File;

import client.Client;
import server.*;
import utility.*;

public class autoTestTransfiguration {
	public static String directory = "/Users/ningli/Desktop/Project/output/";

	public static void main(String[] args) {
		Scanner sc = new Scanner(System.in);
		// get default settings from user
		// cell size
		double cellDegree = 0.1;
		// multiple times for MTP
		double mult = 1;
		// number of PUs/Channels
		int Number_Of_Channels = 1;
		// sides
		int sides = 3;

		System.out.println("Cell size in degree: ");
		cellDegree = sc.nextDouble();
		System.out.println("Multiple times on default MTP function: ");
		mult = sc.nextDouble();
		System.out.println("Number of channels: ");
		Number_Of_Channels = sc.nextInt();
		System.out.println("Polygon sides: ");
		sides = sc.nextInt();

		double ulLat = 38;
		double ulLon = -82;
		double urLat = 38;
		double urLon = -79;
		double llLat = 36;
		double llLon = -82;
		double lrLat = 36;
		double lrLon = -79;
		/*****************************/
		Location upperLeft = new Location(ulLat, ulLon);
		Location upperRight = new Location(urLat, urLon);
		Location lowerLeft = new Location(llLat, llLon);
		Location lowerRight = new Location(lrLat, lrLon);

		// initialization
		GridMap map = new GridMap(upperLeft, upperRight, lowerLeft, lowerRight, cellDegree);
		MTP.ChangeMult(mult);
		Server.Number_Of_Channels = Number_Of_Channels;
		Client.Number_Of_Channels = Number_Of_Channels;

		ArrayList<Double>[] rlist = (ArrayList<Double>[]) new ArrayList[Number_Of_Channels];
		for (int i = 0; i < rlist.length; i++) {
			rlist[i] = new ArrayList<Double>();
		}
		// int[] queries = {0, 20, 40, 60, 80, 100, 120, 140, 160, 180, 200};
		int[] queries = {100, 200, 300, 400, 500, 600, 700, 800, 900, 1000};
		int repeat = 10;

		/* debug information */
		System.out.println("Map length: " + map.getLength() + " km, Map height: " + map.getHeight() + " km");
		System.out.println("map rows: " + map.getRows() + ", map cols: " + map.getCols());
		map.showBoundary();
		System.out.println("Average distance between each cell is: " + map.getAverageDistance() + " km");

	    // initialize a server with parameters from initial settings
		ServerTransfiguration server = new ServerTransfiguration(map, sides);

		/* 
		 * Add a PU to the server's grid map, speficify the PU's location
		 */
		PU pu0 = new PU(0, 9, 9);
		server.addPU(pu0, 0);

		PU pu1 = new PU(1, 9, 50);
		server.addPU(pu1, 1);

		PU pu2 = new PU(2, 30, 9);
		server.addPU(pu2, 1);

		PU pu3 = new PU(3, 30, 50);
		server.addPU(pu3, 0);

		/* debug information */
		System.out.println("Number of PUs: " + server.getNumberOfPUs());
		server.printInfoPU();
		server.printInfoChannel();
		System.out.println();

		// after adding pu, transfigure
		server.transfigure();

		// initiliza a client, then change its location and make a query for N times;
		Client client = new Client(10, 10, map);

		System.out.println("Now perform a series of queries");
		// go thru number of queries in the query list
		for (int q : queries) {
			System.out.println("Number of queries: " + q);
			// specify number of queries for server:
			double[] sumIC = new double[Number_Of_Channels];
			// make queries for certain times
			for (int i = 0; i < repeat; i++) {
				// clear client's probability map to 0.5
				client.reset();
				// set actual lies back to 0
				server.reset();
				for (int j = 0; j < q; j++) {
					client.randomLocation();
					client.query(server);
				}
				double[] IC = client.computeIC(server);
				// System.out.println("IC for channel 0 is " + IC[0]);
				int k = 0;
				for (double ic : IC) {
					sumIC[k] += ic;
					k++;
				}
			}
			// compute average
			int cid = 0;
			for (double ic : sumIC) {
				rlist[cid].add(ic / repeat);
				// System.out.println("Average is " + ic / repeat);
				cid++;
			}
		}

		/* debug information */
		System.out.println("Number of PUs: " + server.getNumberOfPUs());
		server.printInfoPU();
		server.printInfoChannel();
		System.out.println();

		File file = new File(directory + "ic_tf_" + sides + ".txt");
		try {
			PrintWriter out = new PrintWriter(file);
			System.out.println("Start printing... ");
			for (int test : queries) out.print(test + " ");
			out.println();
			for (ArrayList<Double> list: rlist) {
				for (double ic : list) {
					out.print(ic + " ");
				}
				out.println();
			}
			out.close (); // this is necessary
		} catch (FileNotFoundException e) {
			System.err.println("FileNotFoundException: " + e.getMessage());
		} finally {
			System.out.println("Printing succeeded");
		}
	}
}
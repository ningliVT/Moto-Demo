package simulation;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javaPlot.CmpPlot;
import javaPlot.MatPlot;
import server.Server;
import server.ServerAdditiveNoise;
import utility.PU;
import boot.BootParams;
import boot.LatLng;
import client.Client;

public class SimAdditiveNoise extends Simulation {
	private String countermeasure;        // name of countermeasure
	private double noiseLevel;            // noise level, [0, 1]
	private ServerAdditiveNoise cmServer; // instance of countermeasure server
	private int maxIteration;             // max attempts to reach noise level
	private boolean feasible;             // whether this noise level is feasible
	Map<Integer, double[]> icCMMap;;      // ic for multiple simulation with countermeasure

	public SimAdditiveNoise(BootParams bp, double cs, double scale, int inter, String dir) {
		/* call parent constructor */
		super(bp, cs, scale, inter, dir);

		/* initialize countermeasure */
		this.countermeasure = bootParams.getCountermeasure();

		/* initialize noise level */
		this.noiseLevel = bootParams.getCMParam();

		/* initialize server with additive noise */
		cmServer = new ServerAdditiveNoise(map, noc, noq, noiseLevel);
		/* add pu to cmServer */
		int PUid = 0;
		for (int k = 0; k < noc; k++) {
			List<LatLng> LatLngList = bootParams.getPUOnChannel(k);
			for (LatLng ll : LatLngList) {
				PU pu = new PU(PUid++, ll.getLat(), ll.getLng(), map);
				cmServer.addPU(pu, k);
			}
		}

		/* initialize max number of attempts */
		maxIteration = 20;

		/* initialize hashmap for query-ic with countermeasure */
		icCMMap = new HashMap<Integer, double[]>();

		/* initialize feasibility */
		feasible = false;
	}

	@Override
	public void singleSimulation() {
		icq = false;           // do not include ic vs q
		if (noiseLevel < 0 || noiseLevel > 1) {
			System.out.println("Noise level must be in range 0 to 1.");
			feasible = false;   // do not execute following simulations
			return;
		}
		System.out.println("Start querying...");
		
		/* initialize a client */
		Client client = new Client(cmServer);

		/* run simulation for once */
		int attempts = maxIteration;
		while(attempts > 0) {
			// clear client's probability map to 0.5
			client.reset();
			// set actual lies back to 0
			cmServer.reset();
			for (int i = 0; i < noq; i++) {
				client.randomLocation();
				client.query(cmServer);
			}
			if (cmServer.reachNoiseLevel()) {
				System.out.println("Noise level satisfied!");
//				System.out.println("Actual lies: " + cmServer.getNumberOfLies() + " Expected lies: " + cmServer.getExpectedLies());
				break;
			}
			attempts--;
			System.out.println("Noise level not satisfied, try again");
		}
		/* if can't reach noise requirement within 20 attempts, return */
		if (attempts == 0) {
			feasible = false;   // do not execute following simulations
			System.out.println("Noise level is set too high. Requirement can't be reached within 20 attempts.");
			return;
		}
		feasible = true;        // noise level is feasible, proceed
		IC = client.computeIC();
		
		/* debug */
		for (List<PU> puList : cmServer.getChannelsList()) {
			for (PU pu : puList){
				pu.printInfo();
			}
		}
		client.countChannel();
		System.out.println("IC: ");
		for (double d : IC){
			System.out.print((int)d + " ");
		}
		System.out.println();
		
		printSingle(cmServer, client, directory);
	}

	@Override
	public void multipleSimulation() {
		if (!feasible) {
			System.out.println("Noise level is not feasible.");
			icq = false;
			return;
		}
		if (noq < 50) {
			icq = false;
			return;
		}
		icq = true;
		
		/* run multiple simulation without countermeasure */
		super.multipleSimulation();
		System.out.println("Start computing average IC with additive noise...");
		Client multclient = new Client(cmServer);
		// compute query points
		int gap = noq / interval;
		int base = 1;
		int tmp = gap;
		while(tmp / base > 0) {
			base *= 10;
		}
		gap = (gap / (base / 10)) * (base / 10);
		// start query from 0 times
		List<Integer> qlist = new ArrayList<Integer>();
		for (int i = 0; i <= interval + 1; i++) {
			qlist.add(gap * i);
			icCMMap.put(gap * i, new double[noc]);
		}
		/* run simulation for multiple times */
		for (int q : qlist) {             // for each query number
			System.out.println("Number of queries: " + q);
			cmServer.updateLiesNeeded(q); // update expected number of lies
			int attempts = maxIteration;  // with in maxIteration, must succeed once
			int succeed = 0;              // number of successful attempts
			while (attempts > 0 && succeed < repeat) {
				multclient.reset();       // reset matrix to 0.5
				cmServer.reset();         // rest actual lies to 0
				for (int j = 0; j < q; j++) {
					multclient.randomLocation();
					multclient.query(cmServer);
				}
				if (!cmServer.reachNoiseLevel()) {
					System.out.println("Noise condition is not satisfied, try again");
					attempts--; // noise level not reached, bad attempt
				}
				else {
					double[] newIC = multclient.computeIC();
					double[] sum = icCMMap.get(q);
					for (int k = 0; k < noc; k++) {
						sum[k] += newIC[k] / repeat; // avoid overflow
					}
					icCMMap.put(q, sum);
					succeed++; // succeed
					attempts = maxIteration;  // have another [maxIteration] times for next success
				}
			}
			if (attempts == 0) { // can't reach noise level in [maxIteration] attempts
				feasible = false;
				icq = false;
				return;
			}
		}
		printMultiple(qlist, icCMMap, directory, "cmp_AdditiveNoise.txt");
	}

	private String buildMessage() {
		StringBuilder sb = new StringBuilder();
		sb.append("<h3>Simlation_result</h3>");
		sb.append("<p>Distribution_of_primary_users:<br>");
		sb.append("Number_of_PUs:_" + server.getNumberOfPUs() + "<br>");
		sb.append(server.puOnChannelToString()); // which pu is on which channel
		sb.append("</p>");
		if (!feasible) {
			if (noiseLevel > 1 || noiseLevel < 0) {
				sb.append("<p>Noise_level_should_be_in_range_from_0_to_1.</p>");
			}
			else sb.append("<p>Noise_requirement_can't_be_satisfied_because_noise_level_is_set_too_high.</p>");
			return sb.toString();
		}
		sb.append("<p>Querying_information:<br>");
//		sb.append(client.countChannelUpdateToString()); // channel is updated how many times
		sb.append("</p>");
		if (IC != null) {
			sb.append("<p>Inaccuracy_for_each_channel:<br>");
			for (int i = 0; i < IC.length; i++) {
				sb.append("Channel_" + i + ":_" + IC[i] + "<br>");
			}
		}
		sb.append("</p>");
		sb.append("<p>See_probability_plots_in_the_attachments._Location_of_primary_users_are_marked_as_red_stars.</p>");
		return sb.toString();
	}
	
	@Override
	public String getEmailContent() {
		return content.append(buildMessage()).toString();
	}

	/**
	 * Is the given noise level a practical one for this simulation
	 * @return true if noise requirement can be reached 
	 */
	public boolean isFeasible() {
		return feasible;
	}
	
	/**
	 * Whether to plot ic vs q
	 * @return if not feasible, return false
	 */
	@Override
	public boolean plotICvsQuery() {
		if (!feasible) return false;
		return icq;
	}

	public String getCountermeasure() {
		return countermeasure;
	}
}
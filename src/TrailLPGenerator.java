import org.apache.commons.csv.*;
import java.io.*;
import java.util.*;
import java.nio.charset.StandardCharsets;
import lpsolve.*;

// Generates an LP file from various constraints, the loads the file
// into LPSolve and solves it, then prints out relevant information
// about the solution
public class TrailLPGenerator {

	// the trails that will be considered by the solver
	public static List<Trail> trails = new ArrayList<>();

	// the starting longitude and lattitude. By default, Red Square
	public static double startLat = 47.656;
	public static double startLng = -122.309;

	// the input file for the trails, and the output file
	// for the LP file
	public static final String TRAIL_FILE = "trails_with_ratings.csv";
	public static final String OUT_FILE = "trails.lp";

	// the very large value used in the distance
	public static double M = 10000;

	public static int NUM_TRAILS = 5;

	// the maximum distance, in miles, to any one trail
	public static int MAX_DIST = 25;

	// the minimum and maximum total elevation gains for the trails
	public static int ELEV_MIN = 0;
	public static int ELEV_MAX = 999999999;

	// the minimum distance between any two trais, by default
	// no restriction
	public static double MIN_DIST_BETWEEN_TRAILS = 0;

	// the minimum and maximum elevation gains, in feet for any single trail
	public static int SINGLE_ELEV_MIN = 1;
	public static int SINGLE_ELEV_MAX   = 9999999;

	// the maximum length, in miles, for any single trail
	public static int SINGLE_MAX_LENGTH = 9999999;

	// the minimum number of ratings required for a trail
	// to be considered
	public static int MIN_NUM_RATINGS = 1;

	// 0 = sum of number of ratings
	// 1 = sum of ratings
	// 2 = exp(rating) * num_ratings
	// 3 = sum of ratings, each rating squared
	public static int OBJ_FUNC_TYPE = 0;

	public static void main(String[] args) throws IOException {
		int code = 0;
		System.loadLibrary("lpsolve55");
		System.loadLibrary("lpsolve55j");

		// Parse command line options, in a very not-good-code-but-it-works
		// sort of way. Does minimal error checking
		for (int i = 0; i < args.length; i++) {
			String arg = args[i];
			if (arg.equals("-n") && i < args.length - 1) {
				NUM_TRAILS = Integer.parseInt(args[i + 1]);
				i++;
			} else if (arg.equals("-d") && i < args.length - 1) {
				MAX_DIST = Integer.parseInt(args[i + 1]);
				i++;
			} else if (arg.equals("--elev-min") && i < args.length - 1) {
				ELEV_MIN = Integer.parseInt(args[i + 1]);
				i++;
			} else if (arg.equals("--elev-max") && i < args.length - 1) {
				ELEV_MAX = Integer.parseInt(args[i + 1]);
				i++;
			} else if (arg.equals("--single-elev-max") && i < args.length - 1) {
				SINGLE_ELEV_MAX = Integer.parseInt(args[i + 1]);
				i++;
			} else if (arg.equals("--single-length-max") && i < args.length - 1) {
				SINGLE_MAX_LENGTH = Integer.parseInt(args[i + 1]);
				i++;
			} else if (arg.equals("--obj") && i < args.length - 1) {
				OBJ_FUNC_TYPE = Integer.parseInt(args[i + 1]);
				i++;
			} else if (arg.equals("--min-num-ratings") && i < args.length - 1) {
				MIN_NUM_RATINGS = Integer.parseInt(args[i + 1]);
				i++;
			} else if (arg.equals("--min-trail-dist") && i < args.length - 1) {
				MIN_DIST_BETWEEN_TRAILS = Double.parseDouble(args[i + 1]);
				i++;
			} else if (arg.equals("--single-elev-min") && i < args.length - 1) {
				SINGLE_ELEV_MIN = Integer.parseInt(args[i + 1]);
				i++;
			} else if (arg.equals("--start-lat") && i < args.length - 1) {
				startLat = Double.parseDouble(args[i + 1]);
			} else if (arg.equals("--start-lng") && i < args.length - 1) {
				startLng = Double.parseDouble(args[i + 1]);
			}
		}

		// Read in the trails from the CSV file to be considered by the solver
		File data = new File(TRAIL_FILE);
		CSVParser parser = CSVParser.parse(data, StandardCharsets.UTF_8, CSVFormat.EXCEL.withHeader());
		for (CSVRecord record : parser) {
			Trail t = csvToTrail(record);
			// only consider trails if they meet the constraints that only apply
			// to individual trails
			if (trailFilter(t)) {
				trails.add(t);
			}
		}

		// Print out the constraints used in the solution
		System.out.println("num trails: " + NUM_TRAILS);
		System.out.println("max distance to trails: " + MAX_DIST);
		System.out.println("starting point latitude: " + startLat);
		System.out.println("starting point longitude: " + startLng);
		System.out.println("max length of any trail: " + SINGLE_MAX_LENGTH);
		System.out.println("max elev gain of any trail: " + SINGLE_ELEV_MAX);
		System.out.println("min elev gain of any trail: " + SINGLE_ELEV_MIN);
		System.out.println("min total elevation of all trails: " + ELEV_MIN);
		System.out.println("max total elevation of all trails: " + ELEV_MAX);
		System.out.println("min distance between trails: " + MIN_DIST_BETWEEN_TRAILS);
		System.out.println("objective function type: " + OBJ_FUNC_TYPE);

		// Produce the LP file.
		printLPFile(new PrintStream(OUT_FILE));

		// Solve it
		solveAndPrintLP();
	}

	// Prints out the LP equations to a file.
	public static void printLPFile(PrintStream output) {
		output.println(getMaxLine());
		output.println(getTrailNumConstraintLine());
		output.println(getElevLowerLimitLine());
		output.println(getElevUpperLimitLine());
		if (MIN_DIST_BETWEEN_TRAILS > 0) {
			printDistanceContraintLines(output);
		}
		output.println(getVarConstraintLine());
		output.flush();
        output.close();
	}

	// attempts to solve the LP, and prints the solution if there is one
	// By default it uses a timeout of 10 minutes. If an optimal solution
	// is not found in this time, or an exception occurs,
	// it will not print a solution.
	public static void solveAndPrintLP() {
		try {
			LpSolve solver = LpSolve.readLp(OUT_FILE, LpSolve.NORMAL, "trail_lp");
			solver.setTimeout(600); // 10 minutes
			int result = solver.solve();
			if (result == LpSolve.TIMEOUT) {
				System.out.println("timeout");
			} else if (result == LpSolve.SUBOPTIMAL) {
				System.out.println("suboptimal");
			} else {
				double obj = solver.getObjective();
				if (obj < 1e-10) {
					System.out.println("the problem appears infeasible.");
				} else {
					System.out.println("objective: " + obj);
					double[] vars = solver.getPtrVariables();
					for (int i = 0; i < vars.length; i++) {
						double val = vars[i];
						if (val == 1.0) {
							String var = solver.getColName(i + 1);
							if (var.startsWith("x")) {
								int index = Integer.parseInt(var.substring(2));
								Trail t = trails.get(index);
								System.out.println(t.tid + " - " + t.name);
							}
						}
					}
				}
			}
			solver.deleteLp();
		} catch (LpSolveException e) {
			System.out.println("exception occurred :(");
			e.printStackTrace();
		}
	}

	// Print the constraints requiring each trail to be some minimum
	// distance away from every other trail.
	public static void printDistanceContraintLines(PrintStream output) {
		for (int i = 0; i < trails.size(); i++) {
			for (int j = i + 1; j < trails.size(); j++) {
				Trail t1 = trails.get(i);
				Trail t2 = trails.get(j);
				double dist = Trail.distance(t1, t2, 'M');
				String yVar = "y_" + i + "_" + j;
				String x1 = "x_" + i;
				String x2 = "x_" + j;
				output.printf("%s >= %s + %s - 1;\n", yVar, x1, x2);
				output.printf("%s <= %s;\n", yVar, x1);
				output.printf("%s <= %s;\n", yVar, x2);
				output.printf("%.3f - %.3f*%s + %.3f*%s >= %.3f;\n",
					M, M, yVar, dist, yVar, MIN_DIST_BETWEEN_TRAILS
				);
			}
		}
	}

	// Gets the maximization line of the LP solver
	public static String getMaxLine() {
		String line = "max: ";
		List<String> terms = new ArrayList<String>();
		for (int i = 0; i < trails.size(); i++) {
			Trail t = trails.get(i);
			// terms.add(t.rating + "*" + "x_" + i);
			String term = t.rating + "";
			if (OBJ_FUNC_TYPE == 0) {
				term = t.num_ratings + "";
			} else if (OBJ_FUNC_TYPE == 1) {
				term = t.rating + "";
			} else if (OBJ_FUNC_TYPE == 2) {
				term = String.format("%.3f", t.num_ratings * Math.exp(t.rating));
			} else if (OBJ_FUNC_TYPE == 3) {
				term = String.format("%.3f", Math.pow(t.rating, 2));
			}
			terms.add(term + ("*x_" + i));
		}

		line += String.join(" +\n", terms) + ";";
		return line;
	}

	// Gets the constraint line that requires the sum of all the chosen
	// trails to be exactly 5.
	public static String getTrailNumConstraintLine() {
		List<String> terms = new ArrayList<String>();
		for (int i = 0; i < trails.size(); i++) {
			terms.add("x_" + i);
		}
		String line = String.join(" +\n", terms) + " = " + NUM_TRAILS + ";";
		return line;
	}

	// Gets the line that requires the total elevation gain over the
	// 5 hikes to be greater than some minimum
	public static String getElevLowerLimitLine() {
		List<String> terms = new ArrayList<>();
		for (int i = 0; i < trails.size(); i++) {
			Trail t = trails.get(i);
			terms.add(t.elevGain + ("*x_" + i));
		}
		String line = String.join(" +\n", terms) + " >= " + ELEV_MIN + ";";
		return line;
	}

	// Gets the line that requires the total elevation gain over the
	// 5 hikes to be less than some maximum
	public static String getElevUpperLimitLine() {
		List<String> terms = new ArrayList<>();
		for (int i = 0; i < trails.size(); i++) {
			Trail t = trails.get(i);
			terms.add(t.elevGain + ("*x_" + i));
		}
		String line = String.join(" +\n", terms) + " <= " + ELEV_MAX + ";";
		return line;
	}

	// Gets the binary constraint line, requiring all variables to be
	// either 0 or 1.
	public static String getVarConstraintLine() {
		String line = "bin ";
		List<String> terms = new ArrayList<>();
		for (int i = 0; i < trails.size(); i++) {
			terms.add("x_" + i);
		//	System.out.println(terms.get(terms.size() - 1));
		}
		if (MIN_DIST_BETWEEN_TRAILS > 0) {
			for (int i = 0; i < trails.size(); i++) {
				for (int j = i + 1; j < trails.size(); j++) {
					terms.add("y_" + i + "_" + j);
				}
			}
		}
		line += String.join(",\n", terms) + ";";
		return line;
	}

	// Helper method to convert a line from the trail CSV file
	// to a Trail object
	public static Trail csvToTrail(CSVRecord record) {
		Trail t = new Trail();
		t.tid = record.get("tid");
		t.name = record.get("name");
		t.lat = record.get("lat").length() > 0 ? Double.parseDouble(record.get("lat")) : 0.0;
		t.lng = record.get("lng").length() > 0 ? Double.parseDouble(record.get("lng")) : 0.0;
		t.elevGain = record.get("elevGain").length() > 0 ? Double.parseDouble(record.get("elevGain")) : 0.0;
		t.elevMax = record.get("elevMax").length() > 0 ? Double.parseDouble(record.get("elevMax")) : 0.0;
		t.length = record.get("length").length() > 0 ? Double.parseDouble(record.get("length")) : 0.0;
		t.rating = record.get("rating").length() > 0 ? Double.parseDouble(record.get("rating")) : 0.0;
		t.num_ratings = record.get("num_ratings").length() > 0 ? Integer.parseInt(record.get("num_ratings")) : 0;
		t.features = record.get("features");
		t.kml = record.get("kml");
		return t;
	}

	// Filters out trails that don't meet the constraints that apply only to
	// individual trails, so that these don't unnecessarily clutter the
	// LP solver. Trails can be filtered out by the number of ratings on the
	// trails, the elevation gain of the trail, the length of the trail, and
	// the distance from a trail to the starting point.
	public static boolean trailFilter(Trail trail) {
		double distToTrail = Trail.distance(trail.lat, startLat, trail.lng, startLng, 'M');
		return trail.num_ratings >= MIN_NUM_RATINGS
			&& trail.elevGain <= SINGLE_ELEV_MAX
			&& trail.elevGain >= SINGLE_ELEV_MIN
			&& trail.length <= SINGLE_MAX_LENGTH
			&& trail.length > 0
			&& distToTrail <= MAX_DIST;
	}
}

// Defines a wrapper for the data for each trail found from the WTA
// website.
public class Trail {

	// the trail id, but converted to make it acceptable as a name for an
	// LP solver variable (removing spaces, nonalphabetic characters, etc.)
	public String tid_lp;
	public String tid;
	public String name;
	public double lat;
	public double lng;
	public double elevGain;
	public double elevMax;
	public double length;
	public double rating;
	public int num_ratings;
	public String features;
	public String kml;

	// Calculates the distance between two latitude/longitude coordinates.
	// unit: specifies the unit for distance, by default in miles.
	// 'K' returns a value in kilometers, 'N' returns a value in
	// nautical miles
	public static double distance(double lat1,
								  double lat2,
								  double lng1,
								  double lng2,
								  char unit) {
		// if the two points are sufficiently close to each other,
		// just return zero
		if (Math.abs(lat1 - lat2) < 0.000001 && Math.abs(lng1 - lng2) < 0.000001) {
			return 0;
		}

		double theta = lng1 - lng2;
		double dist =
			  Math.sin(Math.toRadians(lat1)) * Math.sin(Math.toRadians(lat2))
			+ Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) * Math.cos(Math.toRadians(theta));

		dist = Math.acos(dist);
		dist = Math.toDegrees(dist);
		dist = dist * 60 * 1.1515;
		if (unit == 'K') {
			dist = dist * 1.609344;
		} else if (unit == 'N') {
			dist = dist * 0.8684;
		}
		return dist;
	}

	// Calculates the distance between two trails
	public static double distance(Trail t1, Trail t2, char unit) {
		return distance(t1.lat, t2.lat, t1.lng, t2.lng, unit);
	}

	// Returns the trail's name as a String representation of the Trail
	public String toString() {
		return name;
	}
}
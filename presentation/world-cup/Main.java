package soccer.simulator;


public class Main {
	public static void main(final String argv[]) {
		
		final String country = argv.length == 0 ? "usa" : argv[0];
		final World_cupWrapper.team instance = World_cupWrapper.create_team(country);
		System.out.println(instance);
	}
}

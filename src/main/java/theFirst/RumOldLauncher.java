package theFirst;

import java.util.Locale;

public class RumOldLauncher {
	public static void main(String[] args) {
		// required Helper Class that avoids Missing Components exception when starting runnable Jar
		// Alternative would be to add this to VM options: --module-path /path/to/JavaFX/lib --add-modules=javafx.controls
		System.out.println("Rum launcher start");
		Locale.setDefault(Locale.UK);
		RumOld.main(args);
	}
}

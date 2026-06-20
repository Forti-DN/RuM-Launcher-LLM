package theFirst;

public class PrologUtils {

    public static String convertToTerm(String s) {
        return s.replace(" ", "_").toLowerCase();
    }

}

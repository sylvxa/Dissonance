package lol.sylvie.dissonance.util;

import lol.sylvie.dissonance.Constants;

public class ConsoleUtil {
    public static void friendlyMessageBox(String... lines) {
        Constants.LOG.error("----------- Dissonance -----------");
        for (String line : lines) {
            Constants.LOG.error(line);
        }
        Constants.LOG.error("----------------------------------");
    }
}

package lol.sylvie.dissonance.permission;

import java.util.Map;

public class DissonancePermissions {
    public static Map<String, Integer> INTS = Map.of(
        "dissonance.purge", 4,
        "dissonance.link", 0,
        "dissonance.unlink", 0,
        "dissonance.linking.bypass_whitelist", 4
    );
}

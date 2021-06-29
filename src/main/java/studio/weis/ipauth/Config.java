package studio.weis.ipauth;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class Config {

    public Map<String, Set<String>> authorized;
    public boolean autoAuth;
    public boolean useUuid;

    public Config() {
        autoAuth = true;
        useUuid = false;
        authorized = new HashMap<>();
    }

    public String serialize() {
        return IpAuth.GSON.toJson(this, Config.class);
    }
}

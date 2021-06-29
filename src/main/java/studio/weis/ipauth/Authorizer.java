package studio.weis.ipauth;

import net.minecraft.server.network.ServerPlayerEntity;

import java.util.*;

public record Authorizer(ConfigManager configManager) {


    public Feedback isAuthorized(ServerPlayerEntity player) {
        String id = configManager.config.useUuid ? player.getUuid().toString() : player.getEntityName();
        String ip = player.getIp();
        Config config = configManager.config;
        if (isIdKnown(id)) {
            Set<String> ips = getIpsFromId(id);
            if (ips.size() == 0) {
                ips.add(ip);
                return new Feedback(true, "");
            }
            return new Feedback(ips.contains(ip), "Sorry, you cannot play from a different IP address.");
        } else if (config.autoAuth) {
            config.authorized.put(id, new HashSet<>(Collections.singleton(ip)));
            configManager.saveConfig();
            return new Feedback(true, "");
        }
        return new Feedback(false, "Sorry, you are unauthorized to play.");
    }

    public Feedback add(List<String> ids, String ips) {
        return add(ids, splitString(ips));
    }

    public Feedback add(List<String> ids, List<String> ips) {
        List<String> lines = new ArrayList<>();
        for (String id : ids) {
            if (isIdKnown(id)) {
                Set<String> ipsSet = getIpsFromId(id);
                List<String> addedIps = new ArrayList<>();
                for (String ip : ips) {
                    if (!ipsSet.contains(ip)) {
                        ipsSet.add(ip);
                        addedIps.add(ip);
                    }
                }
                if (!addedIps.isEmpty()) {
                    lines.add(String.format("Added IP(s) %s to user %s", String.join(", ", addedIps), id));
                }
            } else {
                registerNewUser(id, new HashSet<>(ips));
                if (ips.isEmpty()) {
                    lines.add(String.format("Registered user %s", id));
                } else {
                    lines.add(String.format("Registered user %s with IP(s) %s", id, String.join(", ", ips)));
                }
            }
        }
        if (lines.isEmpty()) {
            return new Feedback(false, "Added nothing...");
        } else {
            configManager.saveConfig();
            return new Feedback(true, String.join("\n", lines));
        }
    }

    public Feedback remove(List<String> ids, String ips) {
        return remove(ids, splitString(ips));
    }

    public Feedback remove(List<String> ids, List<String> ips) {
        List<String> lines = new ArrayList<>();

        for (String id : ids) {
            if (!isIdKnown(id)) {
                lines.add(String.format("Failed to remove unknown user %s.", id));
            } else if (ips.isEmpty()) {
                configManager.config.authorized.remove(id);
                lines.add(String.format("Removed user %s.", id));
            } else {
                Set<String> ipsSet = getIpsFromId(id);
                List<String> removedIps = new ArrayList<>();
                for (String ip : ips) {
                    if (!ipsSet.contains(ip)) {
                        ipsSet.remove(ip);
                        removedIps.add(ip);
                    }
                }
                if (!removedIps.isEmpty()) {
                    lines.add(String.format("Removed IP(s) %s to user %s", String.join(", ", removedIps), id));
                }
            }
        }

        if (lines.isEmpty()) {
            return new Feedback(false, "Removed nothing...");
        } else {
            configManager.saveConfig();
            return new Feedback(true, String.join("\n", lines));
        }
    }

    private Set<String> getIpsFromId(String id) {
        return configManager.config.authorized.get(id);
    }

    private boolean isIdKnown(String id) {
        return configManager.config.authorized.containsKey(id);
    }

    private void registerNewUser(String id, Set<String> ips) {
        configManager.config.authorized.put(id, ips);
    }

    private List<String> splitString(String str) {
        if (str == null) {
            return new ArrayList<>();
        }
        String trimmed = str.trim();
        if (trimmed.isEmpty()) {
            return new ArrayList<>();
        }
        return List.of(trimmed.split("\\s+"));
    }
}

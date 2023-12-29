package studio.weis.ipauth;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.server.command.ServerCommandSource;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static com.mojang.brigadier.arguments.BoolArgumentType.bool;
import static com.mojang.brigadier.arguments.BoolArgumentType.getBool;
import static com.mojang.brigadier.arguments.StringArgumentType.getString;
import static com.mojang.brigadier.arguments.StringArgumentType.greedyString;
import static net.minecraft.command.argument.GameProfileArgumentType.gameProfile;
import static net.minecraft.command.argument.GameProfileArgumentType.getProfileArgument;
import static net.minecraft.server.command.CommandManager.argument;
import static net.minecraft.server.command.CommandManager.literal;

public record CommandHandler(Authorizer authorizer) {
    public CommandHandler(Authorizer authorizer) {
        this.authorizer = authorizer;
        CommandRegistrationCallback.EVENT.register((dispatcher, dedicated, environment) -> {
            dispatcher.register(rootCommand());
        });
    }

    private LiteralArgumentBuilder<ServerCommandSource> rootCommand() {
        return literal(IpAuth.MOD_ID).requires(source -> source.hasPermissionLevel(4))
                .then(literal("add")
                        .then(argument("player", gameProfile()).executes(this::addPlayer)
                                .then(argument("ip", greedyString()).executes(this::addPlayerIp)))
                )
                .then(literal("remove")
                        .then(argument("player", gameProfile()).executes(this::removePlayer)
                                .then(argument("ip", greedyString()).executes(this::removePlayerIp)))
                )
                .then(literal("info").executes(this::info)
                        .then(argument("query", greedyString()).executes(this::infoPlayer))
                )
                .then(literal("setautoauth").then(argument("enabled", bool()).executes(this::setAutoAuth)))
                .then(literal("setuseuuid").then(argument("use", bool()).executes(this::setUseUuid)));
    }

    private List<String> getPlayer(CommandContext<ServerCommandSource> context) {
        try {
            final boolean useUuid = authorizer.configManager().config.useUuid;
            return getProfileArgument(context, "player").stream().map(player -> useUuid ? player.getId().toString() : player.getName()).collect(Collectors.toList());
        } catch (CommandSyntaxException exception) {
            return new ArrayList<>();
        }
    }

    private String getIp(CommandContext<ServerCommandSource> context) {
        return getString(context, "ip");
    }

    private int addPlayer(CommandContext<ServerCommandSource> context) {
        return authorizer.add(getPlayer(context), "").toCommandFeedback(context);
    }

    private int removePlayer(CommandContext<ServerCommandSource> context) {
        return authorizer.remove(getPlayer(context), "").toCommandFeedback(context);
    }

    private int addPlayerIp(CommandContext<ServerCommandSource> context) {
        return authorizer.add(getPlayer(context), getIp(context)).toCommandFeedback(context);
    }

    private int removePlayerIp(CommandContext<ServerCommandSource> context) {
        return authorizer.remove(getPlayer(context), getIp(context)).toCommandFeedback(context);
    }

    private int setAutoAuth(CommandContext<ServerCommandSource> context) {
        Config config = authorizer.configManager().config;
        boolean newValue = getBool(context, "enabled");
        if (newValue == config.autoAuth) {
            return new Feedback(false, "Configuration auto_authorize is already set to " + config.autoAuth + ".").toCommandFeedback(context);
        }
        config.autoAuth = newValue;
        authorizer.configManager().saveConfig();
        String message = newValue ? "From now on, new players will be able to join and get automatically registered with their joining IP address." : "From now on, only authorized players will be able to join the game.";
        return new Feedback(true, message).toCommandFeedback(context);
    }

    private int setUseUuid(CommandContext<ServerCommandSource> context) {
        Config config = authorizer.configManager().config;
        boolean newValue = getBool(context, "use");
        if (newValue == config.useUuid) {
            return new Feedback(false, "Configuration use_uuid is already set to " + config.useUuid + ".").toCommandFeedback(context);
        }
        config.useUuid = newValue;
        authorizer.configManager().saveConfig();
        String message = newValue ? "From now on, the UUID will be used to identify the players." : "From now on, the username will be used to identify the players.";
        return new Feedback(true, message).toCommandFeedback(context);
    }

    private int info(CommandContext<ServerCommandSource> context) {
        Config config = authorizer.configManager().config;
        List<String> lines = List.of(
                "auto_authorize: " + config.autoAuth,
                "use_uuid:" + config.useUuid,
                "authorized: " + String.join(", ", config.authorized.keySet())
        );
        return new Feedback(true, String.join("\n", lines)).toCommandFeedback(context);
    }

    private int infoPlayer(CommandContext<ServerCommandSource> context) {
        Config config = authorizer.configManager().config;
        List<String> lines = new ArrayList<>();
        String query = getString(context, "query");
        config.authorized.forEach((id, ips) -> {
            if (id.contains(query)) {
                lines.add("User search: user " + id + " has access with the following IP(s): " + String.join(", ", ips));
            }
            for (String ip : ips) {
                if (ip.contains(query)) {
                    lines.add("IP search: user " + id + " has access with IP: " + ip);
                }
            }
        });
        if (lines.isEmpty()) {
            return new Feedback(false, "No results were found for both user and IP query.").toCommandFeedback(context);
        }
        return new Feedback(true, String.join("\n", lines)).toCommandFeedback(context);
    }
}

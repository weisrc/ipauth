package studio.weis.ipauth;

import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;


@SuppressWarnings("unused")
public class IpAuth implements ModInitializer {

    public static final Logger LOGGER = LogManager.getLogger();
    public static final Gson GSON = new GsonBuilder().setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES).setPrettyPrinting().create();
    public static final String MOD_ID = "ipauth";
    public static final String MOD_NAME = "Ip Auth";


    @Override
    public void onInitialize() {
        ConfigManager configManager = new ConfigManager();
        Authorizer authorizer = new Authorizer(configManager);
        new CommandHandler(authorizer);
        ServerPlayConnectionEvents.INIT.register(((handler, server) -> {
            Feedback feedback = authorizer.isAuthorized(handler.getPlayer());
            if (!feedback.ok()) {
                handler.disconnect(feedback.getTextOfMessage());
            }
        }));
    }
}

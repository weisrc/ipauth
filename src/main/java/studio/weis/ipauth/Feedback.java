package studio.weis.ipauth;

import com.mojang.brigadier.context.CommandContext;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;

import java.util.function.Supplier;

record Feedback(boolean ok, String message){
    public int toCommandFeedback(CommandContext<ServerCommandSource> context) {
        ServerCommandSource source = context.getSource();
        if (ok) {
            source.sendFeedback(this::getTextOfMessage, true);
        } else {
            source.sendError(getTextOfMessage());
        }
        return ok ? 1 : 0;
    }

    public Text getTextOfMessage() {
        return Text.of(message);
    }
}

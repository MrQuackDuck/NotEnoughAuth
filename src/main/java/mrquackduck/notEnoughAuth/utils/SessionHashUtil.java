package mrquackduck.notEnoughAuth.utils;

import com.destroystokyo.paper.ClientOption;
import io.papermc.paper.connection.PlayerConfigurationConnection;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

@SuppressWarnings({"UnstableApiUsage"})
public abstract class SessionHashUtil {
    public static String getSessionHash(PlayerConfigurationConnection player) {
        String ip = player.getClientAddress().getHostString();
        int viewDistance = player.getClientOption(ClientOption.VIEW_DISTANCE);
        boolean allowServerListings = player.getClientOption(ClientOption.ALLOW_SERVER_LISTINGS);
        String locale = player.getClientOption(ClientOption.LOCALE);
        String chatVisibility = player.getClientOption(ClientOption.CHAT_VISIBILITY).name();
        int skinParts = player.getClientOption(ClientOption.SKIN_PARTS).getRaw();

        return String.valueOf(Math.abs(Objects.hash(ip, viewDistance, allowServerListings, locale, chatVisibility, skinParts)));
    }

    public static @NotNull String getSessionHash(Player player) {
        String ip = player.getAddress().getHostString();
        int viewDistance = player.getClientOption(ClientOption.VIEW_DISTANCE);
        boolean allowServerListings = player.getClientOption(ClientOption.ALLOW_SERVER_LISTINGS);
        String locale = player.getClientOption(ClientOption.LOCALE);
        String chatVisibility = player.getClientOption(ClientOption.CHAT_VISIBILITY).name();
        int skinParts = player.getClientOption(ClientOption.SKIN_PARTS).getRaw();

        return String.valueOf(Math.abs(Objects.hash(ip, viewDistance, allowServerListings, locale, chatVisibility, skinParts)));
    }
}

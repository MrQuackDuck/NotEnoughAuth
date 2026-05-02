package mrquackduck.notEnoughAuth.server.commands;

import io.papermc.paper.dialog.Dialog;
import io.papermc.paper.registry.data.dialog.ActionButton;
import io.papermc.paper.registry.data.dialog.DialogBase;
import io.papermc.paper.registry.data.dialog.action.DialogAction;
import io.papermc.paper.registry.data.dialog.body.DialogBody;
import io.papermc.paper.registry.data.dialog.type.DialogType;
import mrquackduck.notEnoughAuth.NotEnoughAuth;
import mrquackduck.notEnoughAuth.configuration.Configuration;
import mrquackduck.notEnoughAuth.configuration.Permissions;
import mrquackduck.notEnoughAuth.models.AuthResult;
import mrquackduck.notEnoughAuth.models.OAuthSession;
import mrquackduck.notEnoughAuth.models.User;
import mrquackduck.notEnoughAuth.services.OAuthSessionManager;
import mrquackduck.notEnoughAuth.services.authProviders.abstractions.AuthProvider;
import mrquackduck.notEnoughAuth.utils.SessionHashUtil;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.event.ClickEvent;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

@SuppressWarnings({"UnstableApiUsage"})
public class LinkCommand implements CommandExecutor {
    private final NotEnoughAuth plugin;
    private final Configuration config;
    private final OAuthSessionManager sessionManager;

    public LinkCommand(NotEnoughAuth plugin) {
        this.plugin = plugin;
        this.config = new Configuration(plugin);
        this.sessionManager = plugin.getSessionManager();
    }

    @Override
    public boolean onCommand(@NotNull CommandSender commandSender, @NotNull Command command, @NotNull String s, String[] args) {
        if (!(commandSender instanceof Player player)) {
            commandSender.sendMessage(config.getMessage("only-players"));
            return true;
        }

        if (!commandSender.hasPermission(Permissions.LINK)) {
            commandSender.sendMessage(config.getMessage("not-enough-permissions"));
            return true;
        }

        var user = plugin.getUserRepository().findByPlayerName(player.getName());

        var sessionHashFromDb = user.activeSessionHash;
        var currentSessionHash = SessionHashUtil.getSessionHash(player);

        // Compare current session hash and the hash we store
        if (!sessionHashFromDb.equals(currentSessionHash)) {
            player.sendMessage(config.getMessage("rejoin"));
            return true;
        }

        boolean allProvidersLinked = config.authProviders().stream()
                .filter(p -> p.isEnabled)
                .allMatch(p -> user.authenticationData != null &&
                        user.authenticationData.stream().anyMatch(ad -> ad.providerName.equalsIgnoreCase(p.name)));

        if (allProvidersLinked) {
            player.sendMessage(config.getMessage("all-providers-already-linked"));
            return true;
        }

        // Create an OAuth session associated with this player
        var session = sessionManager.createSession(currentSessionHash, user.playerName, OAuthSession.Type.LINK_NEW_ACCOUNT);
        session.onCompleted = result -> {
            if (!result.isSuccess) {
                player.sendMessage(result.errorMessage);
                return;
            }

            sessionManager.remove(currentSessionHash);

            player.sendMessage(config.getMessage("successfully-linked"));
        };

        Dialog dialog = buildDialog(currentSessionHash, user);

        CompletableFuture<AuthResult> response = new CompletableFuture<>();
        response.completeOnTimeout(AuthResult.fail(config.getPlainMessage("timeout")), config.timeout(), TimeUnit.SECONDS);

        session.authFuture = response;

        player.showDialog(dialog);

        return true;
    }

    private Dialog buildDialog(String sessionHash, User user) {
        return Dialog.create(factory -> factory.empty()
                .base(DialogBase.builder(config.getMessage("link-title"))
                        .canCloseWithEscape(false)
                        .body(List.of(
                                DialogBody.plainMessage(config.getMessage("link-description"))
                        ))
                        .build()
                )
                .type(DialogType.multiAction(getAuthButtons(sessionHash, user), null, 1))
        );
    }

    private List<ActionButton> getAuthButtons(String sessionHash, User user) {
        ArrayList<ActionButton> buttons = new ArrayList<>();

        for (AuthProvider provider : config.authProviders()) {
            // Ensure that the provider is enabled in the settings, and it is not already linked to the player's account
            if (provider.isEnabled && user.authenticationData.stream().noneMatch(p -> p.providerName.equalsIgnoreCase(provider.name))) {
                buttons.add(ActionButton.builder(config.getMessage(provider.name.toLowerCase()))
                        .tooltip(config.getMessage("link-hover"))
                        .action(DialogAction.staticAction(ClickEvent.openUrl(provider.buildAuthLink(sessionHash))))
                        .build());
            }
        }

        buttons.add(
                ActionButton.builder(config.getMessage("cancel"))
                        .action(DialogAction.customClick(Key.key("papermc:nea/back"), null))
                        .build());

        return buttons;
    }
}

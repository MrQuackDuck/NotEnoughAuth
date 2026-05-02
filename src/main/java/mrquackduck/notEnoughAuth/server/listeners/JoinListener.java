package mrquackduck.notEnoughAuth.server.listeners;

import io.papermc.paper.connection.PlayerConfigurationConnection;
import io.papermc.paper.dialog.Dialog;
import io.papermc.paper.event.connection.configuration.AsyncPlayerConnectionConfigureEvent;
import io.papermc.paper.event.player.PlayerCustomClickEvent;
import io.papermc.paper.registry.data.dialog.ActionButton;
import io.papermc.paper.registry.data.dialog.DialogBase;
import io.papermc.paper.registry.data.dialog.action.DialogAction;
import io.papermc.paper.registry.data.dialog.body.DialogBody;
import io.papermc.paper.registry.data.dialog.type.DialogType;
import mrquackduck.notEnoughAuth.NotEnoughAuth;
import mrquackduck.notEnoughAuth.configuration.Configuration;
import mrquackduck.notEnoughAuth.models.AuthResult;
import mrquackduck.notEnoughAuth.models.OAuthSession;
import mrquackduck.notEnoughAuth.services.authProviders.abstractions.AuthProvider;
import mrquackduck.notEnoughAuth.services.OAuthSessionManager;
import mrquackduck.notEnoughAuth.utils.SessionHashUtil;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import net.kyori.adventure.key.Key;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;

@SuppressWarnings({"UnstableApiUsage"})
public class JoinListener implements Listener {
    private final NotEnoughAuth plugin;
    private final Configuration config;
    private final OAuthSessionManager sessionManager;

    public JoinListener(NotEnoughAuth plugin) {
        this.plugin = plugin;
        this.config = plugin.getConfiguration();
        this.sessionManager = plugin.getSessionManager();
    }

    // A map for holding all currently connecting players
    private final Map<String, CompletableFuture<AuthResult>> awaitingResponse = new ConcurrentHashMap<>();

    @EventHandler
    void onPlayerConfigure(AsyncPlayerConnectionConfigureEvent event) {
        PlayerConfigurationConnection connection = event.getConnection();
        String playerName = connection.getProfile().getName();

        var sessionHash = SessionHashUtil.getSessionHash(connection);
        var player = plugin.getUserRepository().findByPlayerName(playerName);

        // Proceed if the sessions match
        boolean sessionsMatch = player != null && sessionHash.equals(player.activeSessionHash);

        // Proceed if active provider is still linked to this player's account
        boolean activeProviderStillLinked = player != null && player.authenticationData != null && player.authenticationData.stream().anyMatch(p -> p.providerName.equalsIgnoreCase(player.activeProvider));

        if (sessionsMatch && activeProviderStillLinked) return;

        // Create an OAuth session associated with this player
        var session = sessionManager.createSession(sessionHash, playerName, OAuthSession.Type.AUTHENTICATE);

        Dialog dialog = buildDialog(sessionHash);

        CompletableFuture<AuthResult> response = new CompletableFuture<>();
        response.completeOnTimeout(AuthResult.fail(config.getPlainMessage("timeout")), config.timeout(), TimeUnit.SECONDS);

        session.authFuture = response;
        awaitingResponse.put(sessionHash, response);

        Audience audience = connection.getAudience();
        audience.showDialog(dialog);

        // Wait until completion: either they clicked "Back"/disconnect, or OAuth callback marks success
        AuthResult authResult = waitForAuthOrCancel(response);

        if (!authResult.isSuccess) {
            connection.disconnect(Component.text(authResult.errorMessage));
        }

        awaitingResponse.remove(sessionHash);
        sessionManager.remove(sessionHash);
    }

    private AuthResult waitForAuthOrCancel(CompletableFuture<AuthResult> response) {
        try {
            return response.get();
        } catch (InterruptedException | ExecutionException e) {
            return AuthResult.fail(e.getMessage());
        }
    }

    @EventHandler
    void onHandleDialog(PlayerCustomClickEvent event) {
        if (!(event.getCommonConnection() instanceof PlayerConfigurationConnection configurationConnection)) return;

        var sessionHash = SessionHashUtil.getSessionHash(configurationConnection);

        Key key = event.getIdentifier();
        if (key.equals(Key.key("papermc:nea/disconnect")))
            setConnectionJoinResult(sessionHash, AuthResult.fail(config.getPlainMessage("disconnected")));
    }

    // Simple utility method for setting a connection's dialog response result.
    private void setConnectionJoinResult(String sessionHash, AuthResult result) {
        CompletableFuture<AuthResult> future = awaitingResponse.get(sessionHash);
        if (future != null) {
            future.complete(result);
        }
    }

    private Dialog buildDialog(String sessionHash) {
        return Dialog.create(factory -> factory.empty()
                .base(DialogBase.builder(config.getMessage("title"))
                        .canCloseWithEscape(false)
                        .body(List.of(
                                DialogBody.plainMessage(config.getMessage("description"))
                        ))
                        .build()
                )
                .type(DialogType.multiAction(getAuthButtons(sessionHash), null, 1))
        );
    }

    private List<ActionButton> getAuthButtons(String sessionHash) {
        ArrayList<ActionButton> buttons = new ArrayList<>();

        for (AuthProvider provider : config.authProviders()) {
            if (provider.isEnabled) {
                buttons.add(ActionButton.builder(config.getMessage(provider.name.toLowerCase()))
                        .tooltip(config.getMessage(provider.name.toLowerCase() + "-hover"))
                        .action(DialogAction.staticAction(ClickEvent.openUrl(provider.buildAuthLink(sessionHash))))
                        .build());
            }
        }

        buttons.add(
                ActionButton.builder(config.getMessage("back"))
                        .tooltip(config.getMessage("back-hover"))
                        .action(DialogAction.customClick(Key.key("papermc:nea/disconnect"), null))
                        .build());

        return buttons;
    }
}
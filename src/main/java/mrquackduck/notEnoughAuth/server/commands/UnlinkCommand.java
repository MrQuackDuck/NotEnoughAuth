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
import mrquackduck.notEnoughAuth.models.AuthDataEntry;
import mrquackduck.notEnoughAuth.models.User;
import mrquackduck.notEnoughAuth.utils.SessionHashUtil;
import net.kyori.adventure.text.event.ClickCallback;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

@SuppressWarnings({"UnstableApiUsage"})
public class UnlinkCommand implements CommandExecutor {
    private final NotEnoughAuth plugin;
    private final Configuration config;

    public UnlinkCommand(NotEnoughAuth plugin) {
        this.plugin = plugin;
        this.config = new Configuration(plugin);
    }

    @Override
    public boolean onCommand(@NotNull CommandSender commandSender, @NotNull Command command, @NotNull String s, String[] args) {
        if (!(commandSender instanceof Player player)) {
            commandSender.sendMessage(config.getMessage("only-players"));
            return true;
        }

        if (!commandSender.hasPermission(Permissions.UNLINK)) {
            commandSender.sendMessage(config.getMessage("not-enough-permissions"));
            return true;
        }

        var user = plugin.getUserRepository().findByPlayerName(player.getName());
        var currentSessionHash = SessionHashUtil.getSessionHash(player);

        if (!user.activeSessionHash.equals(currentSessionHash)) {
            player.sendMessage(config.getMessage("rejoin"));
            return true;
        }

        if (activeLinkedProviderCount(user) <= 1) {
            player.sendMessage(config.getMessage("one-provider-left"));
            return true;
        }

        player.showDialog(buildProvidersDialog(user));
        return true;
    }

    private Dialog buildProvidersDialog(User user) {
        return Dialog.create(factory -> factory.empty()
                .base(DialogBase.builder(config.getMessage("unlink-title"))
                        .canCloseWithEscape(false)
                        .body(List.of(DialogBody.plainMessage(config.getMessage("unlink-description"))))
                        .build())
                .type(DialogType.multiAction(buildProviderButtons(user), null, 1))
        );
    }

    private Dialog buildConfirmationDialog(AuthDataEntry authDataEntry) {
        return Dialog.create(factory -> factory.empty()
                .base(DialogBase.builder(config.getMessage("unlink-confirm-title"))
                        .canCloseWithEscape(false)
                        .body(List.of(DialogBody.plainMessage(config.getMessage("unlink-confirm-description"))))
                        .build())
                .type(DialogType.multiAction(buildConfirmationButtons(authDataEntry), null, 2))
        );
    }

    private List<ActionButton> buildProviderButtons(User user) {
        ArrayList<ActionButton> buttons = new ArrayList<>();

        if (user.authenticationData != null) {
            var activeLinkedProviderList = user.authenticationData.stream()
                    .filter(authData -> config.authProviders().stream()
                            .filter(p -> p.isEnabled).anyMatch(p -> p.name.equalsIgnoreCase(authData.providerName))).toList();

            for (AuthDataEntry authDataEntry : activeLinkedProviderList) {
                if (authDataEntry == null || authDataEntry.providerName == null) continue;
                String providerKey = authDataEntry.providerName.toLowerCase();

                buttons.add(ActionButton.builder(config.getMessage(providerKey))
                        .tooltip(config.getMessage("unlink-hover"))
                        .action(DialogAction.customClick(
                                (view, audience) -> {
                                    if (!(audience instanceof Player player)) return;
                                    User freshUser = plugin.getUserRepository().findByPlayerName(player.getName());
                                    if (activeLinkedProviderCount(freshUser) <= 1) {
                                        player.sendMessage(config.getMessage("cant-unlink"));
                                        return;
                                    }

                                    player.showDialog(buildConfirmationDialog(authDataEntry));
                                },
                                ClickCallback.Options.builder().uses(1).build()
                        ))
                        .build());
            }
        }

        buttons.add(ActionButton.builder(config.getMessage("cancel"))
                .action(DialogAction.customClick(
                        (view, audience) -> {},
                        ClickCallback.Options.builder().uses(1).build()
                ))
                .build());

        return buttons;
    }

    private List<ActionButton> buildConfirmationButtons(AuthDataEntry authDataEntry) {
        return List.of(
                ActionButton.builder(config.getMessage("confirm"))
                        .action(DialogAction.customClick(
                                (view, audience) -> {
                                    if (!(audience instanceof Player player)) return;
                                    User freshUser = plugin.getUserRepository().findByPlayerName(player.getName());
                                    if (freshUser == null || freshUser.authenticationData == null) return;
                                    freshUser.authenticationData.removeIf(ad ->
                                            ad != null && ad.providerName != null &&
                                                    ad.providerName.equalsIgnoreCase(authDataEntry.providerName));
                                    plugin.getUserRepository().save(freshUser);
                                    player.sendMessage(config.getMessage("successfully-unlinked"));
                                },
                                ClickCallback.Options.builder().uses(1).build()
                        ))
                        .build(),
                ActionButton.builder(config.getMessage("cancel"))
                        .action(DialogAction.customClick(
                                (view, audience) -> {
                                    if (!(audience instanceof Player player)) return;
                                    User freshUser = plugin.getUserRepository().findByPlayerName(player.getName());
                                    player.showDialog(buildProvidersDialog(freshUser));
                                },
                                ClickCallback.Options.builder().uses(1).build()
                        ))
                        .build()
        );
    }

    private long activeLinkedProviderCount(User user) {
        if (user == null || user.authenticationData == null) return 0;

        return user.authenticationData.stream()
                .filter(authData -> authData != null && authData.providerName != null)
                .filter(authData -> config.authProviders().stream()
                        .filter(p -> p.isEnabled)
                        .anyMatch(p -> p.name.equalsIgnoreCase(authData.providerName)))
                .count();
    }
}
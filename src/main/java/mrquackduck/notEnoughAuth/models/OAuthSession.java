package mrquackduck.notEnoughAuth.models;

import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

public class OAuthSession {
    public final String sessionHash;
    public final String playerName;
    public final Type sessionType;
    public volatile boolean isCompleted;
    public volatile AuthResult result;
    public CompletableFuture<AuthResult> authFuture;

    public Consumer<AuthResult> onCompleted;

    public OAuthSession(String sessionHash, String playerName, Type sessionType) {
        this.sessionHash = sessionHash;
        this.playerName = playerName;
        this.sessionType = sessionType;
        this.isCompleted = false;
        this.result = null;
    }

    public enum Type {
        AUTHENTICATE,
        LINK_NEW_ACCOUNT
    }
}
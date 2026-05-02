package mrquackduck.notEnoughAuth.services;

import mrquackduck.notEnoughAuth.models.AuthResult;
import mrquackduck.notEnoughAuth.models.OAuthSession;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class OAuthSessionManager {
    private final Map<String, OAuthSession> pendingSessions = new ConcurrentHashMap<>();

    public OAuthSession createSession(String state, String playerName, OAuthSession.Type sessionType) {
        OAuthSession session = new OAuthSession(state, playerName, sessionType);
        pendingSessions.put(state, session);
        return session;
    }

    public OAuthSession getByState(String state) {
        return pendingSessions.get(state);
    }

    public void completeSession(String state, AuthResult result) {
        OAuthSession session = pendingSessions.get(state);
        if (session == null) return;

        session.isCompleted = true;
        session.result = result;
        if (session.authFuture != null) {
            session.authFuture.complete(result);
        }

        if (session.onCompleted != null) {
            session.onCompleted.accept(result);
        }
    }

    public void remove(String state) {
        pendingSessions.remove(state);
    }
}
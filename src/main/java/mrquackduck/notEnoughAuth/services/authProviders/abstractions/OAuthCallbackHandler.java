package mrquackduck.notEnoughAuth.services.authProviders.abstractions;

import com.sun.net.httpserver.HttpHandler;
import mrquackduck.notEnoughAuth.models.AuthDataEntry;

public abstract class OAuthCallbackHandler implements HttpHandler {
    public abstract AuthDataEntry fetchAuthData(String code, String state) throws Exception;
}
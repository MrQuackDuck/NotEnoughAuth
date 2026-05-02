package mrquackduck.notEnoughAuth.services.authProviders.abstractions;

public abstract class AuthProvider {
    public final String name;
    public final boolean isEnabled;
    public final String callbackHandlerEndpointPath;
    public final Class<? extends OAuthCallbackHandler> callbackHandlerType;
    public final String clientId;
    public final String secret;
    public final String redirectPath;
    public final String redirectUrl;

    protected AuthProvider(
            String name,
            boolean isEnabled,
            String callbackHandlerEndpointPath,
            Class<? extends OAuthCallbackHandler> callbackHandlerType,
            String clientId,
            String secret,
            String redirectPath,
            String redirectUrl) {
        this.name = name;
        this.isEnabled = isEnabled;
        this.callbackHandlerEndpointPath = callbackHandlerEndpointPath;
        this.callbackHandlerType = callbackHandlerType;
        this.clientId = clientId;
        this.secret = secret;
        this.redirectPath = redirectPath;
        this.redirectUrl = redirectUrl;
    }

    public abstract String buildAuthLink(String state);
}

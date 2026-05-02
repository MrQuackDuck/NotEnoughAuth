package mrquackduck.notEnoughAuth.models;

public class AuthResult {
    public final boolean isSuccess;
    public final String errorMessage;

    private AuthResult(boolean isSuccess, String errorMessage) {
        this.isSuccess = isSuccess;
        this.errorMessage = errorMessage;
    }

    public static AuthResult success() {
        return new AuthResult(true, null);
    }

    public static AuthResult fail(String errorMessage) {
        return new AuthResult(false, errorMessage);
    }
}

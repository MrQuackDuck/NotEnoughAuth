package mrquackduck.notEnoughAuth.models;

import java.util.List;

public class User {
    public String playerName;
    public String activeSessionHash;
    public String activeProvider;

    public List<AuthDataEntry> authenticationData;
}

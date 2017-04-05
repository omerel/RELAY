package com.relay.relay.Util;

import java.util.UUID;

/**
 * Created by omer on 05/04/2017.
 */

public class SearchUser {

    private String fullName;
    private String email;
    private String userName;
    private UUID uuid;

    public SearchUser(String fullName, String email, String userName, UUID uuid) {
        this.fullName = fullName;
        this.email = email;
        this.userName = userName;
        this.uuid = uuid;
    }

    public String getFullName() {
        return fullName;
    }

    public String getEmail() {
        return email;
    }

    public String getUserName() {
        return userName;
    }

    public UUID getUuid() {
        return uuid;
    }
}

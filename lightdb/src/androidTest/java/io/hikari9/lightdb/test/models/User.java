package io.hikari9.lightdb.test.models;

import io.hikari9.lightdb.LightModel;
import io.hikari9.lightdb.annotation.ColumnName;

public class User extends LightModel<User> {

    public String username;
    public String password;
    @ColumnName("email") public String emailAddress;

    public String encrypt(String text) {
        return Integer.toHexString(text.hashCode());
    }

    public User(String username, String rawPassword, String emailAddress) {
        this.username = username;
        this.password = encrypt(rawPassword);
        this.emailAddress = emailAddress;
    }

}

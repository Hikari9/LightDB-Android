package io.hikari9.lightdb.test.models;

import io.hikari9.lightdb.ForeignKey;
import io.hikari9.lightdb.LightModel;

public class UserConnection extends LightModel<UserConnection> {

    public ForeignKey<User> firstUser, secondUser;
    public String type;

    public UserConnection(User firstUser, User secondUser, String type) {
        this.firstUser = new ForeignKey(User.class, firstUser);
        this.secondUser = new ForeignKey(User.class, secondUser);
        this.type = type;
    }

}

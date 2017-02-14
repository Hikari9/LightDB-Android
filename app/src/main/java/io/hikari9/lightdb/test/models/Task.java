package io.hikari9.lightdb.test.models;

import io.hikari9.lightdb.LightModel;

public class Task extends LightModel<Task> {

    String name;
    String action;
    boolean done;

}

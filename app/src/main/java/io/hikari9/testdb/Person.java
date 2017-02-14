package io.hikari9.testdb;

import io.hikari9.lightdb.LightModel;

public class Person extends LightModel<Person> {
    String firstName, lastName;
    public Person() {/* requires empty argument */}
    public Person(String firstName, String lastName) {
        this.firstName = firstName;
        this.lastName = lastName;
    }
}

package io.hikari9.lightdb.testdb;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

import io.hikari9.lightdb.LightDatabase;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        LightDatabase.initialize(this, "test.db", 4, Person.class);
        new Person("First Name", "Last Name").save();
        int sum = LightDatabase.update(Person.class)
            .set("firstName", null)
            .updateAll();
        for (Person person : LightDatabase.all(Person.class)) {
            System.out.println("Hello world: " + person);
        }
    }
}

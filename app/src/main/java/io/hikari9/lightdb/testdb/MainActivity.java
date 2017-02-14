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
        int sum = LightDatabase.update(Person.class)
            .set("firstName", null)
            .updateAll();
        System.out.println("Hello world: " + LightDatabase.select(Person.class).count());
    }
}

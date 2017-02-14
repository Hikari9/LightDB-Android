package io.hikari9.testdb;

import android.database.Cursor;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import io.hikari9.lightdb.LightDatabase;
import io.hikari9.lightdb.LightModel;

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

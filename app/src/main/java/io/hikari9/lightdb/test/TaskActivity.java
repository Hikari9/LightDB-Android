package io.hikari9.lightdb.test;

import android.app.ListActivity;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;
import android.view.View;

import io.hikari9.lightdb.LightDatabase;
import io.hikari9.lightdb.R;
import io.hikari9.lightdb.test.models.Task;

public class TaskActivity extends ListActivity {

    public static final String DATABASE_NAME = "test.db";
    public static final int DATABASE_VERSION = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_task);
        LightDatabase.initialize(this, DATABASE_NAME, DATABASE_VERSION, Task.class);
    }

}

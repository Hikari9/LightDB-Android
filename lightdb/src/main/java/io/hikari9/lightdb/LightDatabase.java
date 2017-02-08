package io.hikari9.lightdb;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class LightDatabase extends SQLiteOpenHelper { // singleton

    private static LightDatabase instance = null;
    public static LightDatabase getInstance() {return instance;}

    protected LightDatabase(Context context, String name, int version) {
        super(context, name, null, version);
    }

    public static void initialize(Context context, String name, int version) {
        instance = new LightDatabase(context, name, version);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        // create tables here
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {

    }

}

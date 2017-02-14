package io.hikari9.lightdb;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

public class LightDatabase extends SQLiteOpenHelper { // singleton

    private static Class<? extends LightModel>[] schema = null;
    private static LightDatabase instance = null;

    // get instance of this database
    public static LightDatabase getInstance() {
        return instance;
    }

    protected LightDatabase(Context context, String name, int version) {
        super(context, name, null, version);
    }

    public static void initialize(Context context, String name, int version, Class<? extends LightModel>... models) {
        if (instance != null) {
            LightDatabase.schema = models;
            instance = new LightDatabase(context, name, version);
        }
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        // create tables
        if (schema != null) {
            for (Class<? extends LightModel> model : schema) {
                LightQuery.createTableIfNotExists(model);
                Log.d("LightDatabase.onCreate", "Created table "
                    + Metadata.fromModel(model).getTableName());
            }
        }
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if (schema != null && oldVersion != newVersion) {
            for (Class<? extends LightModel> model : schema) {
                LightQuery.dropTableIfExists(model);
                LightQuery.createTableIfNotExists(model);
                Log.d("LightDatabase.onUpgrade", "Dropped and recreated table "
                    + Metadata.fromModel(model).getTableName());
            }
        }
    }

}

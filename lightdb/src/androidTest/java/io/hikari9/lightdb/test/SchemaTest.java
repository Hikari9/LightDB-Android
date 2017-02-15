package io.hikari9.lightdb.test;

import android.content.Context;
import android.database.Cursor;
import android.support.test.InstrumentationRegistry;
import android.support.test.internal.runner.junit4.AndroidJUnit4ClassRunner;
import android.support.test.runner.AndroidJUnit4;
import android.util.Log;

import org.hamcrest.Matchers;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Arrays;

import io.hikari9.lightdb.LightDatabase;
import io.hikari9.lightdb.Metadata;
import io.hikari9.lightdb.test.models.User;
import io.hikari9.lightdb.test.models.UserConnection;

@RunWith(AndroidJUnit4.class)
public class SchemaTest {

    public static final String DATABASE_NAME = "test.db";
    public static final int DATABASE_VERSION = 9;

    public Context getContext() {
        return InstrumentationRegistry.getTargetContext();
    }

    @Before
    public void initializeDatabase() {
        LightDatabase.initialize(getContext(), DATABASE_NAME, DATABASE_VERSION);
    }

//    @Test
    public void userFieldNamesTest() {
        // User fields should be the same
        String[] expectedUserColumns = new String[] {"_id", "username", "password", "email"};
        Arrays.sort(expectedUserColumns);
        String[] actualUserColumns = Metadata.fromModel(User.class).getColumnNames();
        Arrays.sort(actualUserColumns);
        Assert.assertArrayEquals(expectedUserColumns, actualUserColumns);
    }

    @Test
    public void createAndDropTableRawTest() {
        LightDatabase.getInstance().getWritableDatabase().execSQL("DROP TABLE IF EXISTS User");
        LightDatabase.getInstance().getWritableDatabase().execSQL("CREATE TABLE IF NOT EXISTS User(_id integer primary key,username,password,email)");
        LightDatabase.getInstance().getWritableDatabase().execSQL("DROP TABLE User");
    }

    @Test
    public void createAndDropTableWithInsertTest() {
        LightDatabase.dropTable(User.class);
        LightDatabase.createTable(User.class);
        Long insertId = LightDatabase.insert(User.class, new User("Rico", "test", "rico@rico.com"));
        Assert.assertEquals(1, LightDatabase.select(User.class).count());
        User rico = LightDatabase.findById(User.class, insertId);
        Assert.assertEquals(rico.username, "Rico");
        Assert.assertEquals(rico.password, rico.encrypt("test"));
        Assert.assertEquals(rico.emailAddress,  "rico@rico.com");
        LightDatabase.dropTable(User.class);
        LightDatabase.createTable(User.class);
        Assert.assertEquals(0, LightDatabase.select(User.class).count());
        rico.save();
        Assert.assertEquals(1, LightDatabase.select(User.class).count());
    }
}

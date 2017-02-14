package io.hikari9.lightdb;

import android.content.ContentValues;
import android.database.Cursor;
import android.util.Log;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

public abstract class LightModel<T extends LightModel> {

    protected Long _id; // Android default primary key is _id by convention
    public static final String ID = "_id"; // convention for ID queries

    public Long getId() {
        return _id;
    }

    /**
     * Create a LightModel instance from a Cursor.
     */
    public static <T extends LightModel> T parseCursor(Class<T> model, Cursor cursor) {
        // create empty instance
        T instance;
        try {
            instance = model.newInstance();
        } catch (InstantiationException|IllegalAccessException e) {
            e.printStackTrace();
            throw new RuntimeException(e.getMessage());
        }
        instance.fromCursor(cursor);
        return instance;
    }

    public void fromCursor(Cursor cursor) {
        // parse assignable fields
        Metadata metadata = Metadata.fromModel(getClass()) ;
        Field[] fields = metadata.getFields();
        String[] columns = metadata.getColumnNames();
        for (int i = 0; i < fields.length; ++i) {
            Class type = fields[i].getType();
            try {
                fields[i].set(this, LightUtils.extractCursorValue(type, cursor, columns[i]));
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            }
        }
    }

    public void fromModel(LightModel<T> model) {
        // copy contents from another model
        for (Field field : Metadata.fromModel(model.getClass()).getFields()) {
            try {
                field.set(this, field.get(model));
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            }
        }
    }

    public ContentValues toContentValues() {
        return LightUtils.createContentValues(this.getClass(), this);
    }

    public Map<String, Object> toMap() {
        Metadata metadata = Metadata.fromModel(getClass());
        String[] columns = metadata.getColumnNames();
        Field[] fields = metadata.getFields();
        Map<String, Object> map = new HashMap<>();
        for (int i = 0; i < fields.length; ++i) {
            try {map.put(columns[i], fields[i].get(this));}
            catch (IllegalAccessException e) {
                e.printStackTrace();
            }
        }
        return map;
    }

    // refresh model from database
    public T refresh() {
        Cursor cursor = LightDatabase.findCursorById(getClass(), getId());
        fromCursor(cursor);
        cursor.close();
        return (T) this;
    }

    // save model to database
    public T save() {
        LightDatabase.upsert(getClass(), this);
        return (T) this;
    }

    @Override
    public String toString() {
        return toContentValues().toString();
    }

}

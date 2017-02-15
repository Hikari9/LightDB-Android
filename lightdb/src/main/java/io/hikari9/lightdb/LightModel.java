package io.hikari9.lightdb;

import android.content.ContentValues;
import android.database.Cursor;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

/**
 * The basic class for an SQLite database table in LightDB (ORM for Android).
 * All SQLite tables must extend this class properly in order to access convenient query building
 * methods.
 *
 * Note that you can assign the table name for a model using @TableName annotation. Otherwise,
 * LightDB will use the name of the class.
 *
 * Example:
 *
 * @TableName("regular_person") // optional
 * public class RegularPerson extends LightModel<Person> {
 *
 *     // by Android convention, all primary keys for all tables will be "_id"
 *     // no need to supply the primary key field, use the getId() method instead
 *
 *     // these fields will be regarded as database columns of this table
 *     String firstName;
 *     String lastName;
 *     boolean isMale;
 *
 *     // you can also ignore certain fields by using the @IgnoreField annotation
 *     @IgnoreField
 *     String nickname;
 *
 *     // you can change the column name of a field depending on convention using @ColumnName
 *     @ColumnName("contact_number")
 *     int contactNumber;
 *
 *     // you can also have foreign keys, use the ForeignKey class
 *     ForeignKey<RegularPerson> spouse;
 *
 * }
 */
public abstract class LightModel<T extends LightModel> {

    // android default primary key is _id by convention
    protected Long _id;

    // a convention for ID queries whenever column name is needed
    public static final String ID = "_id";

    /**
     * Gets the integer primary key value of this LightModel.
     * @return primary key value of this model
     */
    public Long getId() {
        return _id;
    }

    /**
     * Converts a cursor to a LightModel by deriving the field attributes of the class schema.
     * @param model the basis LightModel subclass for parsing
     * @param cursor the database cursor to parse
     * @return a LightModel instance with filled fields based on cursor
     */
    public static <T extends LightModel> T parseCursor(Class<T> model, Cursor cursor) {
        // create empty instance
        T instance = LightModel.createEmptyModel(model);
        instance.fromCursor(cursor);
        return instance;
    }

    /**
     * Creates an empty LightModel instance based on a LightModel subclass, with all fields set to
     * null, without calling any constructor (even the empty constructor).
     * @param model the LightModel subclass to instantiate
     * @return an empty LightModel instance
     */
    public static <T extends LightModel> T createEmptyModel(Class<T> model) {
        return (T) Metadata.fromModel(model).getInstantiator().newInstance();
    }

    /**
     * Dynamically extracts field values from a cursor and assigns them to this LightModel instance.
     * If a field is not present in the cursor, then the field will keep its previous assignment.
     * @param cursor a Cursor to extract field values from
     */
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

    /**
     * Dynamically copies the fields from another model instance.
     * @param model the model to copy from
     */
    public void fromModel(LightModel<T> model) {
        for (Field field : Metadata.fromModel(model.getClass()).getFields()) {
            try {
                field.set(this, field.get(model));
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Converts this model to a ContentValues map of field names to values.
     * @return a ContentValues map of field names to values
     */
    public ContentValues toContentValues() {
        return LightUtils.createContentValues(this.getClass(), this);
    }

    /**
     * Converts this model to a map of field names to values.
     * @return a map of field names to values
     */
    public Map<String, Object> toMap() {
        Metadata metadata = Metadata.fromModel(getClass());
        String[] columns = metadata.getColumnNames();
        Field[] fields = metadata.getFields();
        Map<String, Object> map = new HashMap<>();
        for (int i = 0; i < fields.length; ++i) {
            try {
                map.put(columns[i], fields[i].get(this));
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            }
        }
        return map;
    }

    /**
     * Refreshes the fields of this model from the database.
     * @return this model
     */
    public T refresh() {
        Cursor cursor = LightDatabase.findCursorById(getClass(), getId());
        fromCursor(cursor);
        cursor.close();
        return (T) this;
    }

    /**
     * Save this model to the database. Convenient method for an upsert.
     * @return this model
     */
    public T save() {
        LightDatabase.upsert(getClass(), this);
        return (T) this;
    }

    @Override
    public String toString() {
        return toContentValues().toString();
    }

}

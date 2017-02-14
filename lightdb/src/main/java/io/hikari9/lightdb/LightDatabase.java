package io.hikari9.lightdb;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteQueryBuilder;
import android.util.Log;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import io.hikari9.lightdb.annotation.Index;

public class LightDatabase extends SQLiteOpenHelper { // singleton

    private static LightDatabase instance = null;
    private static Class<? extends LightModel>[] schema = new Class[0];

    // get instance of this database
    protected static LightDatabase getInstance() {
        return instance;
    }

    private LightDatabase(Context context, String name, int version) {
        super(context, name, null, version);
    }

    public static void initialize(Context context, String name, int version, Class<? extends LightModel>... models) {
        schema = models;
        instance = new LightDatabase(context, name, version);
        Log.i("LightDatabase", "Initialized database " + name + " version " + version);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        Log.i("LightDatabase.onCreate", "Creating database "
            + this.getDatabaseName()
            + " (" + schema.length + " tables)");
        for (Class<? extends LightModel> model : schema)
            db.execSQL(createTableQueryString(model));
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        Log.i("LightDatabase.onUpgrade", "Upgrading database "
            + this.getDatabaseName()
            + " from v" + oldVersion
            + " to v" + newVersion
            + " (" + schema.length + " tables)");
        for (Class<? extends LightModel> model : schema)
            db.execSQL("DROP TABLE IF EXISTS " + Metadata.fromModel(model).getTableName());
        onCreate(db);
    }

    // get a cursor pointing to the result of query String
    public static Cursor query(String queryString, Object... bindParams) {
        Cursor cursor = LightDatabase.getInstance()
            .getWritableDatabase()
            .rawQuery(LightUtils.queryParams(queryString, bindParams), null);
        if (cursor == null) return null;
        cursor.moveToFirst();
        return cursor;
    }

    public static void executeQuery(String queryString, Object... bindParams) {
        LightDatabase
            .getInstance()
            .getWritableDatabase()
            .execSQL(LightUtils.queryParams(queryString, bindParams), null);
    }

    public static Cursor readQuery(String queryString, Object... bindParams) {
        Cursor cursor = LightDatabase.getInstance()
            .getReadableDatabase()
            .rawQuery(LightUtils.queryParams(queryString, bindParams), null);
        if (cursor == null) return null;
        cursor.moveToFirst();
        return cursor;
    }

    // Perform a select query and convert the results to the specified model.
    public static <T extends LightModel> List<T> all(Class<T> model, String queryString, Object... bindParams) {
        Cursor cursor = readQuery(queryString, bindParams);
        if (cursor == null)
            return new ArrayList<>();
        List<T> results = new ArrayList<>();
        for (; !cursor.isAfterLast(); cursor.moveToNext()) {
            results.add(LightModel.parseCursor(model, cursor));
        }
        cursor.close();
        return results;
    }

    public static <T extends LightModel> List<T> all(Class<T> model) {
        return all(model, "SELECT * FROM " + Metadata.fromModel(model).getTableName());
    }

    // Perform a select query and convert the single row result
    public static <T extends LightModel> T one(Class<T> model, String queryString, Object... bindParams) {
        Cursor cursor = readQuery(queryString, bindParams);
        if (cursor.getCount() == 0) return null;
        T result = LightModel.parseCursor(model, cursor);
        cursor.close();
        return result;
    }

    public static void dropTable(String tableName) {
        executeQuery("DROP TABLE IF EXISTS" + tableName);
        Log.i("LightDatabase.dropTable", "Successfully dropped table " + tableName);
    }

    public static void dropTable(Class<? extends LightModel> model) {
        String tableName = Metadata.fromModel(model).getTableName();
        dropTable(tableName);
    }

    public static void createTable(Class<? extends LightModel> model) {
        executeQuery(createTableQueryString(model));
        Log.i("LightDatabase", "Successfully created table " + Metadata.fromModel(model).getTableName());
    }

    public static String createTableQueryString(Class<? extends LightModel> model) {
        StringBuilder builder = new StringBuilder();
        Metadata metadata = Metadata.fromModel(model);
        String tableName = metadata.getTableName();
        builder.append("CREATE TABLE IF NOT EXISTS ");
        builder.append(tableName);
        builder.append("(");
        boolean first = true;
        for (Field field : metadata.getFields()) {
            if (!first) builder.append(",");
            else first = false;
            String fieldName = field.getName();
            builder.append(fieldName);
            if (fieldName.equals(LightModel.ID))
                builder.append(" integer primary key");
            if (field.isAnnotationPresent(Index.class))
                builder.append(" index");
        }
        builder.append(")");
        return builder.toString();
    }

    public static <T extends LightModel> LightDatabase.Builder.Update<T> update(Class<T> model) {
        return new LightDatabase.Builder.Update<>(model);
    }

    // Create a query builder for deleting
    public static <T extends LightModel> LightDatabase.Builder.Delete<T> delete(Class<T> model) {
        return new LightDatabase.Builder.Delete<>(model);
    }

    // delete an object by id
    public static boolean deleteById(Class<? extends LightModel> model, Long id) {
        return id != null && 1 == LightDatabase.getInstance()
            .getWritableDatabase()
            .delete(Metadata.fromModel(model).getTableName(), LightModel.ID + "=" + id, null);
    }

    public static <T extends LightModel> boolean update(Class<T> model, LightModel instance) {
        if (instance.getId() == null) return false;
        String tableName = Metadata.fromModel(model).getTableName();
        ContentValues contentValues = instance.toContentValues();
        return 1 == LightDatabase.getInstance()
            .getWritableDatabase()
            .update(tableName, contentValues, "id=" + instance.getId(), new String[0]);
    }

    // Perform insert into query. Returns NULL if query failed.
    public static <T extends LightModel> Long insert(Class<T> model, LightModel instance) {
        SQLiteDatabase database = LightDatabase.getInstance().getWritableDatabase();
        Metadata metadata = Metadata.fromModel(model);
        String tableName = metadata.getTableName();
        ContentValues contentValues = LightUtils.createContentValues(model, instance);
        long insertId = database.insert(tableName, null, contentValues);
        if (insertId == -1)
            return null;
        instance._id = insertId;
        return insertId;
    }


    // Perform an update query, otherwise perform an insert.
    public static <T extends LightModel> Long upsert(Class<T> model, LightModel instance) {
        if (!update(model, instance))
            return insert(model, instance);
        return instance.getId();
    }

    // find specified model instance by id
    public static <T extends LightModel> T findById(Class<T> model, Long id) {
        Cursor cursor = findCursorById(model, id);
        if (cursor == null || cursor.getCount() != 1) return null;
        T instance = LightModel.parseCursor(model, cursor);
        cursor.close();
        return instance;
    }

    public static Cursor findCursorById(Class<? extends LightModel> model, Long id) {
        if (id == null) return null;
        return readQuery("SELECT * FROM "
            + Metadata.fromModel(model).getTableName()
            + " WHERE id=" + id);
    }

    // perform a select query
    public static <T extends LightModel> Builder.Select<T> select(Class<T> model, String... columns) {
        return new LightDatabase.Builder.Select<T>(model, columns);
    }

    // QueryBuilder class for select statements
    public static abstract class Builder {

        public abstract String getQueryString();

        protected static abstract class Where<T extends Where> extends Builder {

            List<String> whereClause = new ArrayList<>();
            /* WHERE CLAUSES START */

            public T where(String queryString) {
                whereClause.add("(" + queryString + ")");
                return (T) this;
            }

            // note: multiple values denote an WHERE column=values[0] OR column=values[1] OR ...
            public T whereEquals(String column, Object... values) {
                if (values == null || values.length == 0)
                    return (T) this; // don't do anything
                StringBuilder builder = new StringBuilder();
                for (Object value : values) {
                    if (builder.length() > 0)
                        builder.append(" OR ");
                    if (value == null)
                        builder.append(column).append(" IS NULL");
                    else
                        builder.append(column).append("=").append(LightUtils.toSafeString(value));
                }
                whereClause.add(builder.toString());
                return (T) this;
            }

            // note: multiple values denote an WHERE column=values[0] OR column=values[1] OR ...
            public T whereNotEquals(String column, Object value) {
                if (value == null)
                    whereClause.add(column + " IS NOT NULL");
                else
                    whereClause.add(column + "<>" + LightUtils.toSafeString(value));
                return (T) this;
            }

            public T whereLessThan(String column, Object value) {
                whereClause.add(column + "<" + LightUtils.toSafeString(value));
                return (T) this;
            }

            public T whereLessThanOrEquals(String column, Object value) {
                whereClause.add(column + "<=" + LightUtils.toSafeString(value));
                return (T) this;
            }

            public T whereGreaterThan(String column, Object value) {
                whereClause.add(column + ">" + LightUtils.toSafeString(value));
                return (T) this;
            }

            public T whereGreaterThanOrEquals(String column, Object value) {
                whereClause.add(column + ">" + LightUtils.toSafeString(value));
                return (T) this;
            }

            public T whereBetween(String column, Object lowerBound, Object upperBound) {
                whereClause.add(column + " BETWEEN " + lowerBound + " AND " + upperBound);
                return (T) this;
            }

            /* WHERE CLAUSES END */
            public String getQueryString() {
                if (whereClause.size() > 0) {
                    return " WHERE " + LightUtils.join(" AND ", whereClause);
                }
                return "";
            }
        }

        public static class Delete<T extends LightModel> extends Where<Delete<T>> {

            Class<T> model;

            Delete(Class<T> model) {
                this.model = model;
            }

            public int performDelete() {
                return LightDatabase.getInstance()
                    .getWritableDatabase()
                    .delete(
                        Metadata.fromModel(model).getTableName(),
                        LightUtils.join(" AND ", whereClause),
                        null
                    );
            }

            @Override
            public String getQueryString() {
                return "DELETE FROM " + Metadata.fromModel(model).getTableName() + super.getQueryString();
            }
        }

        public static class Update<T extends LightModel> extends Where<Update<T>> {

            Class<T> model;
            ContentValues contentValues;

            protected Update(Class<T> model) {
                this.model = model;
                this.contentValues = new ContentValues();
            }

            public Update<T> set(String column, Object value) {
                LightUtils.putContentValue(contentValues, column, value);
                return this;
            }

            public Update<T> unset(String column) {
                contentValues.remove(column);
                return this;
            }

            // return the first updated value as a model
            public T updateOne() {
                if (updateAll() >= 1) {
                    Long id = contentValues.getAsLong(LightModel.ID);
                    return LightDatabase.findById(model, id);
                }
                return null;
            }

            public int updateAll() {
                return LightDatabase.getInstance()
                    .getWritableDatabase()
                    .update(
                        Metadata.fromModel(model).getTableName(),
                        contentValues,
                        LightUtils.join(" AND ", whereClause),
                        null
                    );
            }

            @Override
            public String getQueryString() {
                StringBuilder builder = new StringBuilder();
                builder.append("UPDATE ");
                builder.append(Metadata.fromModel(model).getTableName());
                builder.append(" SET ");
                boolean first = true;
                for (Map.Entry<String, Object> entry : contentValues.valueSet()) {
                    if (!first) builder.append(",");
                    else first = false;
                    String key = entry.getKey();
                    Object value = entry.getValue();
                    builder.append(key).append("=").append(LightUtils.toSafeString(value));
                }
                builder.append(super.getQueryString());
                return builder.toString();
            }
        }

        public static class Insert<T extends LightModel> extends Builder {

            Class<T> model;
            ContentValues contentValues;

            protected Insert(Class<T> model) {
                this.model = model;
                this.contentValues = new ContentValues();
            }

            public Insert<T> set(String column, Object value) {
                LightUtils.putContentValue(contentValues, column, value);
                return this;
            }

            public T performInsert() {
                long insertId = LightDatabase.getInstance()
                    .getWritableDatabase()
                    .insert(Metadata.fromModel(model).getTableName(), null, contentValues);
                if (insertId == -1)
                    return null;
                return LightDatabase.findById(model, insertId);
            }

            @Override
            public String getQueryString() {
                StringBuilder builder = new StringBuilder();
                Metadata metadata = Metadata.fromModel(model);
                String tableName = metadata.getTableName();
                String[] columns = metadata.getColumnNames();
                builder.append("INSERT INTO ")
                    .append(tableName)
                    .append("(")
                    .append(LightUtils.join(",", columns))
                    .append(") VALUES (");
                boolean first = true;
                for (String column : columns) {
                    if (!first) builder.append(",");
                    else first = false;
                    builder.append(contentValues.get(column));
                }
                builder.append(")");
                return builder.toString();
            }

        }

        public static class Select<T extends LightModel> extends Where<Select<T>> {

            Class<T> model;
            String[] columns;
            Integer rowLimit = null;
            Integer rowOffset = null;
            List<String> orderClause = new ArrayList<>();

            Select(Class<T> model, String[] columns) {
                this.model = model;
                this.columns = columns;
            }

            public Select<T> orderBy(String... columns) {
                orderClause.addAll(Arrays.asList(columns));
                return this;
            }

            public Select<T> limit(Integer rowLimit) {
                this.rowLimit = rowLimit;
                return this;
            }

            public Select<T> offset(Integer rowOffset) {
                this.rowOffset = rowOffset;
                return this;
            }

            /* CONVENIENCE QUERY FUNCTIONS */
            public Cursor cursor() {
                return LightDatabase.readQuery(getQueryString());
            }

            public T one() {
                if (columns.length > 0) {
                    Log.w("warn", "'*' will be used for SELECT query to parse correct model");
                    String[] columnsBefore = columns;
                    columns = new String[0];
                    T result = LightDatabase.one(this.model, getQueryString());
                    columns = columnsBefore;
                    return result;
                }
                return LightDatabase.one(this.model, getQueryString());
            }

            public List<T> all() {
                if (columns.length > 0) {
                    Log.w("warn", "'*' will be used for SELECT query to parse correct model");
                    String[] columnsBefore = columns;
                    columns = new String[0];
                    List<T> results = LightDatabase.all(this.model, getQueryString());
                    columns = columnsBefore;
                    return results;
                }
                return LightDatabase.all(this.model, getQueryString());
            }

            public int count() {
                Cursor cursor = this.cursor();
                int rows = cursor.getCount();
                cursor.close();
                return rows;
            }

            @Override
            public String getQueryString() {
                StringBuilder builder = new StringBuilder();
                builder.append("SELECT ");
                if (columns == null || columns.length == 0)
                    builder.append("*");
                else
                    SQLiteQueryBuilder.appendColumns(builder, columns);
                builder.append(" FROM ").append(Metadata.fromModel(model).getTableName());
                // add where clause
                if (whereClause.size() > 0)
                    builder.append(" WHERE ").append(LightUtils.join(" AND ", whereClause));
                // add order by clause
                if (orderClause.size() > 0)
                    builder.append(" ORDER BY ").append(LightUtils.join(",", orderClause));
                // add extra modifiers
                if (rowLimit != null)
                    builder.append(" LIMIT ").append(rowLimit);
                if (rowOffset != null)
                    builder.append(" OFFSET ").append(rowOffset);
                return builder.toString();
            }

        }

    }

}

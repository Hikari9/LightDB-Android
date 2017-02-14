package io.hikari9.lightdb;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteQueryBuilder;
import android.util.Log;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import io.hikari9.lightdb.meta.Metadata;

public class LightQuery {

    // get metadata from model
    public static Metadata meta(Class<? extends LightModel> model) {
        return Metadata.fromModel(model);
    }

    // get a cursor pointing to the result of query String
    public static Cursor rawQuery(String queryString, Object... bindParams) {
        return LightDatabase.getInstance()
            .getReadableDatabase()
            .rawQuery(LightUtils.queryParams(queryString, bindParams), null);
    }

    // Perform a select query and convert the results to the specified model.
    public static <T extends LightModel> List<T> all(Class<T> model, String queryString, Object... bindParams) {
        Cursor cursor = rawQuery(queryString, bindParams);
        if (cursor == null)
            return new ArrayList<>();
        List<T> results = new ArrayList<>();
        for (cursor.moveToFirst(); !cursor.isAfterLast(); cursor.moveToNext()) {
            results.add(LightModel.parseCursor(model, cursor));
        }
        cursor.close();
        return results;
    }

    // Perform a select query and convert the single row result
    public static <T extends LightModel> T one(Class<T> model, String queryString, Object... bindParams) {
        Cursor cursor = rawQuery(queryString, bindParams);
        if (cursor == null)
            return null;
        cursor.moveToFirst();
        if (cursor.getCount() == 0)
            return null;
        T result = LightModel.parseCursor(model, cursor);
        cursor.close();
        return result;
    }

    // Perform insert into query. Returns NULL if query failed.
    public static Long insert(Class<? extends LightModel> model, LightModel<LightModel> object) {
        SQLiteDatabase database = LightDatabase.getInstance().getWritableDatabase();
        long insertId = database.insert(
            Metadata.fromModel(model).getTableName(),
            null,
            LightUtils.createContentValues(model, object)
        );
        if (insertId == -1)
            return null;
        return insertId;
    }

    // Update the database version of the object
    public static <T extends LightModel> boolean update(Class<T> model, LightModel<LightModel> object) {
        if (object.getId() == null) {
            // cannot perform an update
            return false;
        }
        SQLiteDatabase database = LightDatabase.getInstance().getWritableDatabase();
        String tableName = Metadata.fromModel(model).getTableName();
        int rowsAffected = database.update(
            tableName,
            object.toContentValues(),
            LightModel.ID + "=" + object.getId(),
            new String[0]
        );
        return rowsAffected > 0;
    }

    public static <T extends LightModel> LightQuery.Builder.Update<T> update(Class<T> model) {
        return new LightQuery.Builder.Update<>(model);
    }

    // Create a query builder for deleting
    public static <T extends LightModel> LightQuery.Builder.Delete<T> delete(Class<T> model) {
        return new LightQuery.Builder.Delete<>(model);
    }

    // delete an object by id
    public static boolean deleteById(Class<? extends LightModel> model, Long id) {
        return id != null && 1 == LightDatabase.getInstance()
            .getWritableDatabase()
            .delete(Metadata.fromModel(model).getTableName(), LightModel.ID + "=" + id, null);
    }

    // Perform an update query, otherwise perform an insert.
    public static <T extends LightModel> Long upsert(Class<T> model, LightModel<LightModel> object) {
        if (!update(model, object)) {
            Long insertId = insert(model, object);
            String tableName = Metadata.fromModel(model).getTableName();
            object.fromCursor(rawQuery("SELECT * FROM " + tableName + " WHERE id=?", insertId));
            return insertId;
        }
        return object.getId();
    }

    // find specified model instance by id
    public static <T extends LightModel> T findById(Class<T> model, Long id) {
        if (id == null)
            return null;
        String tableName = Metadata.fromModel(model).getTableName();
        return one(model, "SELECT * FROM " + tableName + " WHERE id=?", id);
    }

    // perform a select query
    public static <T extends LightModel> Builder.Select<T> select(Class<T> model, String... columns) {
        return new LightQuery.Builder.Select<T>(model, columns);
    }

    // QueryBuilder class for select statements
    static abstract class Builder {

        public abstract String getQueryString();

        private static abstract class Where<T extends Where> extends Builder {

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

        static class Delete<T extends LightModel> extends Where<Delete<T>> {

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

            public int performUpdate() {
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

        static class Select<T extends LightModel> extends Where<Select<T>> {

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
                return LightQuery.rawQuery(getQueryString());
            }

            public T one() {
                if (columns.length > 0) {
                    Log.w("warn", "'*' will be used for SELECT query to parse correct model");
                    String[] columnsBefore = columns;
                    columns = new String[0];
                    T result = LightQuery.one(this.model, getQueryString());
                    columns = columnsBefore;
                    return result;
                }
                return LightQuery.one(this.model, getQueryString());
            }

            public List<T> all() {
                if (columns.length > 0) {
                    Log.w("warn", "'*' will be used for SELECT query to parse correct model");
                    String[] columnsBefore = columns;
                    columns = new String[0];
                    List<T> results = LightQuery.all(this.model, getQueryString());
                    columns = columnsBefore;
                    return results;
                }
                return LightQuery.all(this.model, getQueryString());
            }

            @Override
            public String getQueryString() {
                StringBuilder builder = new StringBuilder();
                builder.append("SELECT ");
                if (columns == null || columns.length == 0)
                    builder.append("*");
                else
                    SQLiteQueryBuilder.appendColumns(builder, columns);
                builder.append(" ").append(Metadata.fromModel(model).getTableName());
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

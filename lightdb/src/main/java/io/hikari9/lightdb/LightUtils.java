package io.hikari9.lightdb;

import android.content.ContentValues;
import android.database.Cursor;

import java.lang.reflect.Field;
import java.util.List;

public class LightUtils {

    /* CONVERSION UTILS */

    private static final Class[] integerClasses = new Class[] {
        int.class, short.class, byte.class, long.class, boolean.class,
        Integer.class, Short.class, Byte.class, Long.class, Boolean.class
    };

    private static final Class[] floatClasses = new Class[] {
        float.class, Float.class, double.class, Double.class
    };

    public static boolean isInteger(Class cls) {
        for (Class check : integerClasses)
            if (cls == check)
                return true;
        return false;
    }

    public static boolean isFloat(Class cls) {
        for (Class check : floatClasses)
            if (cls == check)
                return true;
        return false;
    }

    public static String toSafeString(Object object) {
        if (isInteger(object.getClass()) || isFloat(object.getClass()))
            return object.toString();
        if (object instanceof LightModel)
            return ((LightModel) object).getId().toString();
        return "'" + object.toString().replaceAll("'", "''") + "'";
    }

    public static void putContentValue(ContentValues contentValues, String key, Object value) {
        Class<?> type = value.getClass();
        if (value == null)
            contentValues.putNull(key);
        else if (type == long.class || type == Long.class)
            contentValues.put(key, (Long) value);
        else if (type == int.class || type == Integer.class)
            contentValues.put(key, (Integer) value);
        else if (type == String.class)
            contentValues.put(key, (String) value);
        else if (type == double.class || type == Double.class)
            contentValues.put(key, (Double) value);
        else if (type == float.class || type == Float.class)
            contentValues.put(key, (Float) value);
        else if (type.isAssignableFrom(ForeignKey.class)) // foreign key
            contentValues.put(key, ((LightModel) value).getId());
        else if (type == boolean.class || type == Boolean.class)
            contentValues.put(key, (Boolean) value);
        else if (type == byte.class || type == Byte.class)
            contentValues.put(key, (Byte) value);
        else if (type == short.class || type == Short.class)
            contentValues.put(key, (Short) value);
        else if (type == byte[].class)
            contentValues.put(key, (byte[]) value);
        else
            contentValues.put(key, value.toString());
    }

    public static Object extractCursorValue(Class type, Cursor cursor, String columnName) {
        int columnId = cursor.getColumnIndex(columnName);
        if (columnId == -1) return null;
        if (cursor.isNull(columnId)) return null;
        else if (type == long.class || type == Long.class) return cursor.getLong(columnId);
        else if (type == int.class || type == Integer.class) return cursor.getInt(columnId);
        else if (type == String.class) return cursor.getString(columnId);
        else if (type == double.class || type == Double.class) return cursor.getDouble(columnId);
        else if (type == float.class || type == Float.class) return cursor.getFloat(columnId);
        else if (type.isAssignableFrom(ForeignKey.class)) // foreign key
            return new ForeignKey<>(type.getClass(), cursor.getLong(columnId));
        else if (type == boolean.class || type == Boolean.class) return cursor.getInt(columnId) != 0;
        else if (type == byte.class || type == Byte.class) return (byte) cursor.getInt(columnId);
        else if (type == short.class || type == Short.class) return cursor.getShort(columnId);
        else if (type == byte[].class) return cursor.getBlob(columnId);
        else return cursor.getString(columnId);
    }


    public static String queryParams(String query, Object... params) {
        StringBuilder queryBuilder = new StringBuilder();
        int ptr = 0;
        for (int i = 0; i < query.length(); ++i) {
            char ch = query.charAt(i);
            if (ch == '?' && ptr < params.length)
                queryBuilder.append(LightUtils.toSafeString(params[ptr++]));
            else
                queryBuilder.append(ch);
        }
        return queryBuilder.toString();
    }

    public static String join(String delim, String... params) {
        StringBuilder builder = new StringBuilder();
        boolean first = true;
        for (String param : params) {
            if (!first) builder.append(delim);
            else first = false;
            builder.append(param);
        }
        return builder.toString();
    }

    public static String join(String delim, List<String> params) {
        return join(delim, params.toArray(new String[0]));
    }

    // have this to get content values BASED ON MODEL (not from object.getClass())
    public static ContentValues createContentValues(Class<? extends LightModel> model, LightModel object) {
        Metadata meta = Metadata.fromModel(model);
        ContentValues contentValues = new ContentValues();
        String[] columns = meta.getColumnNames();
        Field[] fields = meta.getFields();
        for (int i = 0; i < fields.length; ++i) {
            try {
                LightUtils.putContentValue(contentValues, columns[i], fields[i].get(object));
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            }
        }
        return contentValues;
    }

}
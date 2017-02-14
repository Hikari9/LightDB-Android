package io.hikari9.lightdb.meta;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.hikari9.lightdb.LightModel;
import io.hikari9.lightdb.annotation.ColumnName;
import io.hikari9.lightdb.annotation.IgnoreField;
import io.hikari9.lightdb.annotation.TableName;

public class Metadata {

    private String tableName;
    private Class<? extends LightModel> model;
    private Field[] fields;
    private String[] columns;
    private static final Map<Class<? extends LightModel>, Metadata> instances = new HashMap<>();

    // derive metadata object from model
    public static Metadata fromModel(Class<? extends LightModel> model) {
        if (instances.containsKey(model))
            return instances.get(model);
        Metadata metadata = new Metadata(model);
        instances.put(model, metadata);
        return metadata;
    }

    public Class<? extends LightModel> getModel() {
        return model;
    }

    /**
     * Get the table name used for SQLite transactions for this model. Returns the annotated value
     * of @TableName if provided, otherwise returns the class name by default.
     */
    public String getTableName() {
        return tableName;
    }

    /**
     * Get the list of all fields associated with the model.
     */
    public Field[] getFields() {
        return fields.clone();
    }

    /**
     * Get named field associated with the model.
     */
    public Field getField(String fieldName) {
        for (Field field : fields)
            if (field.getName().equals(fieldName))
                return field;
        return null;
    }

    /**
     * Get the column names associated with the model.
     */
    public String[] getColumnNames() {
        return columns.clone();
    }

    /**
     * Acquire all declared fields of this class.
     * @param model
     * @param fieldList
     * @param columnList
     */
    private static void parseModelMeta(Class model, List<Field> fieldList, List<String> columnList) {
        while (model != null) {
            for (Field field : model.getDeclaredFields()) {
                if (!field.isAnnotationPresent(IgnoreField.class)) {
                    fieldList.add(field);
                    ColumnName columnName = field.getAnnotation(ColumnName.class);
                    if (columnName != null) columnList.add(columnName.value());
                    else columnList.add(field.getName());
                }
            }
            model = model.getSuperclass();
        }
    }

    /**
     * Prepare the LightMeta instance by deriving properties from Model class.
     * @param model
     */
    private Metadata(Class<? extends LightModel> model) {

        // setup matadata for this model
        this.model = model;

        // check if custom table name is present, otherwise use the class name
        this.tableName = model.isAnnotationPresent(TableName.class)
            ? model.getAnnotation(TableName.class).value()
            : model.getSimpleName();

        // Collect all fields recursively
        List<Field> fieldList = new ArrayList<>();
        List<String> columnList = new ArrayList<>();
        parseModelMeta(model, fieldList, columnList);
        this.fields = fieldList.toArray(new Field[0]);
        this.columns = columnList.toArray(new String[0]);

    }
}
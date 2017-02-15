package io.hikari9.lightdb;

import android.database.Cursor;
import android.support.annotation.Nullable;

/**
 * A class to wrap a foreign key pointer to another database model.
 * @param <T> wrap a primary key pointer to another LightModel
 */
public class ForeignKey<T extends LightModel> {

    // reference to the model class used for model retrieval queries
    Class<T> model;

    // id of entity pointed by this foreign key
    Long id;

    /**
     * Construct a foreign key reference based on a LightModel and a given id (can be null).
     * @param model the class instance of the database model referred by this foreign key
     * @param id the id of the entity pointed by this foreign key
     */
    public ForeignKey(Class<T> model, @Nullable Long id) {
        this.model = model;
        this.id = id;
    }

    /**
     * Sets the id of this foreign key.
     * @param id the id of the entity pointed by this foreign key
     */
    public void setId(@Nullable Long id) {
        this.id = id;
    }

    /**
     * Gets the id of this foreign key.
     * @return id the id of the entity pointed by this foreign key
     */
    public Long getId() {
        return id;
    }

    /**
     * Sets the id of this foreign key based on referred database model instance.
     * @param instance a LightModel object that this foreign key will refer
     */
    public void setModel(T instance) {
        if (instance == null) setId(null);
        else setId(instance.getId());
    }

    /**
     * Gets the model referred by this foreign key.
     * @return the LightModel object referred by this foreign key, null if reference does not exist
     */
    public T getModel() {
        return LightDatabase.findById(model, id);
    }

    /**
     * Gets a Cursor to the query used to retrieve the entity referred by this foreign key.
     * @return a Cursor object that this foreign key refers
     */
    public Cursor getModelCursor() {
        return LightDatabase.findCursorById(model, id);
    }

    @Override
    public int hashCode() {
        return id.hashCode() * 27 + model.hashCode();
    }

    @Override
    public String toString() {
        return "ForeignKey<" + model.getSimpleName() + ":" + getId() + ">";
    }
}

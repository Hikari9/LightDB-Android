package io.hikari9.lightdb;

import android.database.Cursor;

public class ForeignKey<T extends LightModel> {
    Class<T> model;
    Long id;
    ForeignKey(Class<T> model, Long id) {
        this.model = model;
        this.id = id;
    }
    void setId(Long id) {
        this.id = id;
    }
    Long getId() {
        return id;
    }
    T retrieve() {
        return LightDatabase.findById(model, id);
    }
    Cursor retrieveCursor() {
        return LightDatabase.findCursorById(model, id);
    }
}

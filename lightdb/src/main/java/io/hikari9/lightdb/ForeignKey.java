package io.hikari9.lightdb;

/**
 * Created by rico on 2/14/17.
 */

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
        return LightQuery.findById(model, id);
    }
}

package com.edulink.gui.interfaces;

import java.util.List;

/**
 * Generic CRUD interface.
 * Each service must implement these 4 methods.
 */
public interface IService<T> {
    void add(T t);
    void edit(T t);
    void delete(int id);
    List<T> getAll();
}

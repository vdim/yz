/*
 *  Copyright (C) 2011 Petrozavodsk State University
 *  
 *  This file is part of Nest.
 */

package ru.petrsu.nest.son;

import clojure.lang.RT;
import clojure.lang.Var;
import java.io.FileNotFoundException;
import java.util.Collection;
import ru.petrsu.nest.yz.core.ElementManager;

/**
 * Manage SON structure in a local data store.
 *
 * @author Mikhail Kryshen
 */
public class LocalSonManager implements ElementManager {
    private static final String SON_NS = "ru.petrsu.nest.son";
    private static final String PLANTER_NS = "net.kryshen.planter.core";
    private static final String ROOT_ID = "root";

    static {
        try {
            RT.load(SON_NS);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static final Var factory = RT.var(PLANTER_NS, "bean-instance");
    private static final Var load = RT.var(PLANTER_NS, "properties");
    private static final Var getPropertyValue = RT.var(PLANTER_NS, "get-value");
    private static final Var register = RT.var(PLANTER_NS, "register-bean");
    private static final Var save = RT.var(PLANTER_NS, "save-all");
    private static final Var saveAndWait = RT.var(PLANTER_NS, 
                                                  "save-all-and-wait");
    private static final Var instancesOf = RT.var(PLANTER_NS, "instances-of");
    private static final Var sonDefaults = RT.var(SON_NS, "SON:defaults");

    public SON getSON() {
        SON son = (SON) factory.invoke(SON.class, ROOT_ID);

        try {
            load.invoke(son);
        } catch (Exception e) {
            if (e instanceof FileNotFoundException) {
                System.err.println("Root element not found: " + e);
                son = new SON(ROOT_ID, sonDefaults.invoke());
                register.invoke(son);
                System.err.println("Created new SON root element.");
                //save.invoke();
            } else {
                throw new RuntimeException(e);
            }
        }

        return son;
    }

    @SuppressWarnings("unchecked")
    public <T extends SonElement> Collection<T> getElements(Class<T> type) {
        return (Collection<T>) instancesOf.invoke(type);
    }

    public void requestSave() throws Exception {
        save.invoke();
    }

    public void saveAndWait() throws Exception {
        try {
            saveAndWait.invoke();
        } catch (Exception e) {
            throw new Exception(e);
        }
    }
    
    // Implementation the ElementManager interface. Needed for queries.

    @Override
    public Collection getClasses () {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @SuppressWarnings("unchecked")
    @Override
    public Collection getElems(Class clazz) {
        return getElements(clazz);
    }
    
    @Override
    @SuppressWarnings("unchecked")
    public Object getPropertyValue (Object o, String property) {
        return (Object) getPropertyValue.invoke(o, property);
    }
}

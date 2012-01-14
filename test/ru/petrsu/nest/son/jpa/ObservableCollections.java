/*
 *  Copyright (C) 2008 Petrozavodsk State University
 *  
 *  This file is part of Nest.
 */

package ru.petrsu.nest.son.jpa;

import java.util.Collection;
import java.util.Iterator;
import java.util.Set;
import org.hibernate.Hibernate;

/**
 *
 *
 * @author Mikhail Kryshen
 */
public class ObservableCollections {
    
    public static <E> Collection<E> observableCollection(Collection<E> c,
            CollectionObserver<? super E> observer) {
        
        return new ObservableCollection<E>(c, observer);
    }
    
    public static <E> Set<E> observableSet(Set<E> set,
            CollectionObserver<? super E> observer) {
        
        return new ObservableSet<E>(set, observer);
    }
    
    private static class ObservableCollection<E>
            extends AbstractObservableCollection<E, Collection<E>> {
        
        public ObservableCollection(Collection<E> collection,
                CollectionObserver<? super E> observer) {
            super(collection, observer);
        }
    }
    
    private static class ObservableSet<E>
            extends AbstractObservableCollection<E, Set<E>>
            implements Set<E> {
        
        public ObservableSet(Set<E> set, CollectionObserver<? super E> observer) {
            super(set, observer);
        }
    }
    
    public static <T> void compareSets(
            Set<? extends T> oldSet, Set<? extends T> newSet,
            CollectionObserver<T> observer) {
        
        for (T e: oldSet) {
            if (!newSet.contains(e)) {
                observer.elementRemoved(e, newSet);
            }
        }
        
        for (T e: newSet) {
            if (!oldSet.contains(e)) {
                observer.elementAdded(e, newSet);
            }
        }
    }

    /**
     * Modify target set to contain the same elements as source set.
     */
    public static <T> void unifySets(Set<T> target, Set<T> source,
            CollectionObserver<T> observer) {

        Iterator<T> ti = target.iterator();
        while (ti.hasNext()) {
            T e = ti.next();

            if (!source.contains(e)) {
                ti.remove();
                observer.elementRemoved(e, source);
            }
        }

        for (T e : source) {
            if (target.add(e)) {
                observer.elementAdded(e, source);
            }
        }
    }
    
    public static boolean isInitialized(Collection c) {
        if (c instanceof AbstractObservableCollection)
            return Hibernate.isInitialized(
                    ((AbstractObservableCollection)c).collection);
        return Hibernate.isInitialized(c);
    }
    
}

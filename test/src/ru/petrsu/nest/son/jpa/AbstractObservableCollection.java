/*
 *  Copyright (C) 2008 Petrozavodsk State University
 *  
 *  This file is part of Nest.
 */

package ru.petrsu.nest.son.jpa;

import java.util.AbstractCollection;
import java.util.Collection;
import java.util.Iterator;

/**
 * Decorates generic Collection to make the changes observable.
 *
 * @author Mikhail Kryshen
 */
abstract class AbstractObservableCollection<E, C extends Collection<E>>
        extends AbstractCollection<E> {
    
    protected C collection;
    protected CollectionObserver<? super E> observer;
    
    AbstractObservableCollection(C collection,
            CollectionObserver<? super E> observer) {
        
        this.collection = collection;
        this.observer = observer;
    }
    
    public C getCollection() {
        return collection;
    }
    
    public Iterator<E> iterator() {
        final Iterator<E> iterator = collection.iterator();
        
        return new Iterator<E> () {
            private E last;
            
            public boolean hasNext() {
                return iterator.hasNext();
            }
            
            public E next() {
                last = iterator.next();
                return last;
            }
            
            public void remove() {
                iterator.remove();
                removed(last);
            }
        };
    }
    
    @Override
    public boolean add(E e) {
        if (collection.add(e)) {
            added(e);
            return true;
        }
        
        return false;
    }
    
    @SuppressWarnings("unchecked")
    @Override
    public boolean remove(Object o) {
        if (collection.remove(o)) {
            removed((E) o);
            return true;
        }
        
        return false;
    }
    
    @Override
    public boolean contains(Object o) {
        return collection.contains(o);
    }
    
    @Override
    public boolean containsAll(Collection<?> c) {
        return collection.containsAll(c);
    }
    
    public int size() {
        return collection.size();
    }
    
    protected void added(E e) {
        observer.elementAdded(e, this);
    }
    
    protected void removed(E e) {
        observer.elementRemoved(e, this);
    }
}

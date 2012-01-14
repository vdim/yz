/*
 *  Copyright (C) 2008 Petrozavodsk State University
 *  
 *  This file is part of Nest.
 */

package ru.petrsu.nest.son.jpa;

import java.util.Collection;

/**
 * Interface to observe changes in a collection.
 *
 * @author Mikhail Kryshen
 */
public interface CollectionObserver<E> {
    
    void elementAdded(E e, Collection<? extends E> observable);
    
    void elementRemoved(E e, Collection<? extends E> observable);
}

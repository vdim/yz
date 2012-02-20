/*
 *  Copyright (C) 2008 Petrozavodsk State University
 *  
 *  This file is part of Nest.
 */

package ru.petrsu.nest.son;

import java.beans.BeanInfo;
import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Queue;
import java.util.Set;

/**
 * Utility methods for dealing with SON beans.
 *
 * @author Mikhail Kryshen
 */
public class SonBeanUtils {
    /**
     * Breadth-first search (BFS) iterator for SON structure.
     * Algorithm is modified to avoid querying for the next node's children
     * until there are non-visited nodes on the current level.
     */
    public static class BreadthFirstIterator implements Iterator<SonElement> {
        private Queue<SonElement> visitQueue = new ArrayDeque<SonElement>();
        private Deque<SonElement> expandQueue = new ArrayDeque<SonElement>();
        
        private Map<SonElement, Integer> visited = 
                new HashMap<SonElement, Integer>();
        
        private final int maxDepth;
        private final boolean close;
        private boolean skipped = false;
        
        private SonElement expand = null;
        private int depth = 0;
        
        /**
         * @param start element to start traversal from
         * @param maxDepth limits maximum depth level
         * @param close revisit elements with several paths from start.
         */
        public BreadthFirstIterator(SonElement start, int maxDepth,
                                    boolean close) {
            visitQueue.add(start);
            this.maxDepth = maxDepth;
            this.close = close;
        }

        public BreadthFirstIterator(SonElement start, int maxDepth) {
            this(start, maxDepth, false);
        }

        public BreadthFirstIterator(SonElement start) {
            this(start, -1);
        }

        private void expand() {
            while (!expandQueue.isEmpty() && visitQueue.isEmpty()) {
                expand = expandQueue.remove();                
                depth = visited.get(expand) + 1;

                if (maxDepth >= 0 && depth > maxDepth) {
                    continue;
                }                

                for (SonElement r : relatedElements(expand)) {
                    if (close) {
                        Integer d = visited.get(r);
                        if (d == null || d.intValue() != depth - 2) {
                            visitQueue.add(r);
                        }
                    } else if (!visited.containsKey(r)) {
                        visitQueue.add(r);
                    }
                }
            }
        }

        public boolean hasNext() {
            expand();
            return !visitQueue.isEmpty();
        }

        public SonElement next() {
            expand();

            if (visitQueue.isEmpty())
                throw new NoSuchElementException();

            SonElement next = visitQueue.remove();

            if (visited.put(next, depth) == null)
                expandQueue.add(next);

            skipped = false;

            return next;
        }
        
        /**
         * Do not continue traversal in depth from the current node.
         */
        public void skip() {
            if (skipped)
                throw new IllegalStateException();

            expandQueue.removeLast();
            skipped = true;
        }
        
        /**
         * Returns the last element that was traversed in depth.
         * May return different value after hasNext() or next() call.         
         */
        public SonElement getLastExpanded() {
            return expand;
        }
        
        /**
         * Returns the depth level of the last traversed element.
         * May return different value after hasNext() or next() call.
         */
        public int getLevel() {
            return depth;
        }
        
        /**
         * Not supported.
         */
        public void remove() {
            throw new UnsupportedOperationException("Not supported yet.");
        }
    }
    
    /**
     * Find path betwean two elements.
     * 
     * @param from Starting element.
     * @param to Target element.
     * @return List of all elements in the path.
     */
    public static List<SonElement> findPath(SonElement from, SonElement to) {
        List<SonElement> front1 = new ArrayList<SonElement>();
        List<SonElement> front2 = new ArrayList<SonElement>();
        Set<SonElement> visited = new HashSet<SonElement>();
        Map<SonElement, SonElement> wayback =
                new HashMap<SonElement, SonElement>();

        front1.add(from);
        visited.add(from);

        search:
        while (true) {
            for (SonElement u : front1) {
                for (SonElement v : relatedElements(u)) {
                    if (visited.contains(v))
                        continue;

                    wayback.put(v, u);

                    if (v == to)
                        break search;

                    front2.add(v);
                    visited.add(v);
                }
            }

            if (front2.isEmpty()) {
                // No path exists.
                return null;
            }

            List<SonElement> t = front1;
            front1 = front2;
            front2 = t;
            front2.clear();
        }

        List<SonElement> path = new LinkedList<SonElement>();
        SonElement last = to;

        while (true) {
            path.add(0, last);

            if (last == from)
                break;

            last = wayback.get(last);
        }

        return path;
    }  
    
    /**
     * Returns Iterator for the elements directly related to the specified
     * element (all the element's property values which are also SON elements).
     *
     * @param element The element.
     */
    @SuppressWarnings("unchecked")
    public static Iterator<SonElement> relatedElementsIterator(
            final SonElement element) {

        return new Iterator<SonElement>() {
            private BeanInfo beanInfo = null;
            private PropertyDescriptor[] pds = null;
            
            private int index = 0;
            private SonElement next = null;
            
            private Iterator pRelated = null;

            private void findNextElement() {
                if (beanInfo == null) {
                    try {
                        beanInfo =
                                Introspector.getBeanInfo(element.getClass());
                        pds = beanInfo.getPropertyDescriptors();
                    } catch (IntrospectionException ex) {
                        throw new RuntimeException(ex);
                    }
                }

                while (index < pds.length &&
                        (pRelated == null || !pRelated.hasNext())) {
                    
                    nextProperty();
                }

                if (pRelated.hasNext()) {
                    next = (SonElement) pRelated.next();
                    return;
                }
            }

            private void nextProperty() {
                PropertyDescriptor desc = pds[index++];
                Method read = desc.getReadMethod();

                if (read == null) {
                    return;
                }

                Class<?> type = desc.getPropertyType();

                try {
                    if (SonElement.class.isAssignableFrom(type)) {
                        SonElement r =
                                (SonElement) read.invoke(element);
                        
                        if (r != null) {
                            pRelated = Collections.singleton(r).iterator();
                        }
                        return;
                    }

                    Class<?> elementType = getElementType(type, read);

                    if (elementType != null &&
                            SonElement.class.isAssignableFrom(elementType)) {
                        
                        pRelated = Arrays.asList(
                               (Object[]) read.invoke(element)).iterator();
                        return;
                    }
                } catch (IllegalArgumentException ex) {
                    throw new RuntimeException(ex);
                } catch (IllegalAccessException ex) {
                    throw new RuntimeException(ex);
                } catch (InvocationTargetException ex) {
                    throw new RuntimeException(ex);
                } catch (IntrospectionException ex) {
                    throw new RuntimeException(ex);
                }
            }

            public boolean hasNext() {
                if (next == null)
                    findNextElement();
                
                return next != null;
            }

            public SonElement next() {
                if (next == null)
                    findNextElement();
                
                if (next == null)
                    throw new NoSuchElementException();
                
                try {
                    return next;
                } finally {
                    next = null;
                }
            }

            public void remove() {
                throw new UnsupportedOperationException("Not supported yet.");
            }
        };
    }

   /**
     * Returns the elements directly related to the specified element
     * (all the element's property values which are also SON elements).
     *
     * @param e The element.
     */
    public static Iterable<SonElement> relatedElements(final SonElement e) {
        return new Iterable<SonElement>() {
            @Override
            public Iterator<SonElement> iterator() {
                return relatedElementsIterator(e);
            }
        };
    }

    public static Class<?> getElementType(PropertyDescriptor pd)
            throws IntrospectionException {

        return getElementType(pd.getPropertyType(), pd.getReadMethod());
    }

    public static Class<?> getElementType(Class<?> type, Method readMethod)
            throws IntrospectionException {        

        if (type.isArray()) {
            return type.getComponentType();
        }

        if (!Collection.class.isAssignableFrom(type)) {
            return null;
        }

        Type t = readMethod.getGenericReturnType();

        if (!(t instanceof ParameterizedType)) {
            return null;
        }

        ParameterizedType pt = (ParameterizedType) t;

        Type[] atas = pt.getActualTypeArguments();

        if (atas.length < 1) {
            return null;
        }

        if (!(atas[0] instanceof Class)) {
            return null;
        }

        return (Class) atas[0];
    }

    static String capitalize(String name) {
        if (name == null || name.isEmpty()) {
            return name;
        }

        return Character.toUpperCase(name.charAt(0)) + name.substring(1);
    }
}

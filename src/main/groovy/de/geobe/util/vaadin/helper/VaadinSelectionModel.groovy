/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2018.  Georg Beier. All rights reserved.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package de.geobe.util.vaadin.helper

import de.geobe.util.vaadin.type.VaadinSelectionListener
import de.geobe.util.vaadin.type.VaadinTreeRootChangeListener

/**
 * Default implementation and delegate for VaadinSelectionModels Listeners.<br>
 *     Supports different kinds of objects within one selection component, e.g. a Tree.
 *     We suppose that the relevant item ids consist of a Tuple2 with a key as first element
 *     (e.g. a class name) and a value as second element (e.g. an object id from the persistent storage).
 *     ListenersForKey subscribe for a certain key and are notified with the value when an
 *     item with "their" key is selected. KeyListeners get notified with the key when any
 *     item with a Map-id is selected.<br>
 * @author georg beier
 */
class VaadinSelectionModel<T extends IdProvider<Tuple2>> {
    private Map<String, Set<VaadinSelectionListener>> keySelectiveListeners = new LinkedHashMap<>()
    private Set<VaadinSelectionListener> anyKeyListeners = new LinkedHashSet<>()
    private Set<VaadinTreeRootChangeListener> rootChangeListeners = new LinkedHashSet<>()

    public void addListenerForKey(VaadinSelectionListener l, String key) {
        if (!keySelectiveListeners.containsKey(key)) {
            keySelectiveListeners[key] = new LinkedHashSet<VaadinSelectionListener>()
        }
        keySelectiveListeners[key].add(l)
    }

    public void removeListenerForKey(VaadinSelectionListener l, String key) {
        keySelectiveListeners[key]?.remove(l)
    }

    public void addAnyKeyListener(VaadinSelectionListener keyListener) {
        anyKeyListeners.add(keyListener)
    }

    public void removeAnyKeyListener(VaadinSelectionListener keyListener) {
        anyKeyListeners.remove(keyListener)
    }

    public void addRootChangeListener(VaadinTreeRootChangeListener changeListener) {
        rootChangeListeners.add(changeListener)
    }

    public void removeRootChangeListener(VaadinTreeRootChangeListener changeListener) {
        rootChangeListeners.remove(changeListener)
    }

    public void notifyChange(T rawEvent) {
        keySelectiveListeners[rawEvent.id.first()].each {
            it.onItemSelected(rawEvent)
        }
        anyKeyListeners.each { it.onItemSelected(rawEvent) }
    }

    public void notifyRootChange(T rawEvent) {
        rootChangeListeners.each { it.onRootChanged(rawEvent) }
    }
}

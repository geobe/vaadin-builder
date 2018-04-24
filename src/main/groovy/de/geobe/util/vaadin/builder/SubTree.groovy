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

package de.geobe.util.vaadin.builder

import com.vaadin.ui.Component
import com.vaadin.ui.UI

/**
 *     A base class for building Vaadin component subtrees with VaadinBuilder.<br>
 *     See here for a
 *     <a href="https://www.georgbeier.de/docs-and-howtos/vaadin_builder_doc/#subtree" target="_blank">
 *     detailed description and tutorial</a> on the VaadinBuilder documentation page.<br>
 * @author georg beier
 */
abstract class SubTree {
    /**
     * builder is configured here and used in subclasses
     */
    protected VaadinBuilder vaadin
    /**
     * make prefix available for accessing components in the subclasses
     */
    protected String subkeyPrefix
    protected def uiComponents

    /**
     * set component prefix in builder, delegate building subtree to subclass
     * and reset prefix afterwards.
     * @param builder VaadinBuilder instance that builds the whole GUI
     * @param componentPrefix name prefix for components in this subtree
     * @return topmost component (i.e. root) of this subtree
     */
    Component buildSubtree(VaadinBuilder builder, String componentPrefix) {
        this.vaadin = builder
        def oldKeyPrefix = builder.getKeyPrefix()
        subkeyPrefix = oldKeyPrefix + componentPrefix
        builder.setKeyPrefix subkeyPrefix
        Component component = build()
        builder.setKeyPrefix oldKeyPrefix
        component
    }

    /**
     * build component subtree.
     * @return topmost component (i.e. root) of subtree
     */
    abstract Component build()

    /**
     * initialize subtree components. should be called after whole component tree is built.
     * call sequence of different subtrees may be important.
     * @param value various parameters needed for initialization
     */
    void init(Object... value) {}

    /**
     * find the top level UI component of a given component
      * @param c here we start to look
     * @return the corresponding UI instance
     */
    protected UI getVaadinUi(Component c) {
        Component parent = c?.parent
        if (parent instanceof UI) {
            parent
        } else {
            getVaadinUi(parent)
        }
    }

    /**
     * catch conversion exceptions from String to Long
     */
    protected Long longFrom(String val) {
        try {
            new Long(val)
        } catch (NumberFormatException e) {
            0L
        }
    }

    /**
     * easy access to components in this component subtree without
     * need to concat subkeyPrefix
     * @return matching component
     */
    protected subtreeComponent(String id) {
        vaadin.uiComponents["${subkeyPrefix + id}"]
    }
}
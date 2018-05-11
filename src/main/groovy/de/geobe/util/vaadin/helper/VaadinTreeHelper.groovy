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

import com.vaadin.data.TreeData
import com.vaadin.data.provider.TreeDataProvider
import com.vaadin.ui.Tree

/**
 * A set of utility methods to make work with Vaadin Tree component a bit easier
 * <br>
 * @author georg beier
 */
class VaadinTreeHelper<T extends IdProvider> {
    private Tree<T> tree
    private TreeData<T> treeData

    /**
     * A TreeHelper instance is bound to a Vaadin Tree
     * @param aTree the Tree object that is supported by this instance
     */
    public VaadinTreeHelper(Tree<T> aTree) {
        tree = aTree
    }

    /**
     *  add a new node to the tree
     * @param id of a new node. if null, id will be generated
     * @param parentId id of parent node. if null, there is no parent
     * @param caption caption of new node as displayed
     * @param childrenAllowed can node have children?
     * @return id , either given or generated
     */
    public addNode(Object id, Object parentId, String caption, Boolean childrenAllowed) {
        id
    }

    /**
     * build a vaadin tree for a given tree data structure. To ease the work with tree
     * selections, id's for the vaadin tree node consist of Tuple2 objects with a
     * key describing the kind of object represented by the node and a value, typically
     * a database key or other unique value used to lookup the domain instance
     * represented by this tree node
     * @param roots an array of root nodes
     * @param kidCollector closure that extracts a collection of child nodes
     */
    public void buildTree(Collection<T> roots, Closure<Collection<T>> kidCollector) {
        treeData = new TreeData<>()
        TreeDataProvider<T> provider = new TreeDataProvider<>(treeData)
        addToTree(null, roots, kidCollector)
        tree.dataProvider = provider
    }

    /**
     * recursively add the child nodes
     * @param treeData the TreeData structure to be built
     * @param base current base node (null for root nodes)
     * @param kids child nodes
     * @param kidCollector Closure that extracts grandchild nodes from kids
     */
    private void addToTree(T base, Collection<T> kids,
                           Closure<Collection<T>> kidCollector) {
//        def b = base ? base.id : null
        kids.each { T kid ->
            treeData.addItem(base, kid)
        }
        kids.each { T kid ->
            def kidKids = kidCollector(kid)
            addToTree(kid, kidKids, kidCollector)
        }
    }

    /**
     * find the node id of the topmost node for a given node
     * @param node the node where we start
     * @return topmost parent node
     */
    public topParentForId(def node) {
        def parent = treeData.getParent(node)
        if (parent)
            topParentForId(parent)
        else
            node
    }

    /**
     * expand all nodes of the tree
     */
    public void expandAll() {
        tree.expand allItems()
    }

    /**
     * get a list of all items in the tree
     *
     * @return a List of all treenode items
     */
    public allItems() {
        accumulateItems treeData.rootItems
    }

    /** recursively walk the tree */
    private accumulateItems(List<T> items) {
        def accu = []
        items.each {
            accu.add it
            accu.addAll accumulateItems(treeData.getChildren(it))
        }
        accu
    }

    public void clear() {
        treeData.clear()
    }

    /**
     * get a List that contains all expanded nodes.
     *
     * @return the expanded node list
     */
    public getIdsOfExpanded() {
        def expanded = allItems().findAll {T node ->
            def ex = tree.isExpanded(node)
            ex
        }
        def expIds = expanded.collect {T it -> it.id}
        expIds
    }

    /**
     * try to identify previously expanded nodes from List and reexpand them after
     * a tree reload. As node objects differ after reload, this needs a
     * comparison in groovy
     *
     * @param exp the list generated before tree reload
     */
    public void reexpand(Collection exp) {
        def toExpand = allItems().findAll { T node ->
            exp.contains node.id
        }
        tree.expand toExpand
    }

    /**
     * look for new node with the same id as previously selected node
     * before tree reload and select it
     * @param id the id of node to be selected
     */
    public void reselect(def id) {
        if(id) {
            T selectedNode = allItems().find { T node ->
                id == node.id
            }
            if (selectedNode) {
                tree.select selectedNode
            }
        }
    }

    /**
     * find matching itemId in the tree by comparing each id with the
     * match parameter. This is necessary because Tree uses Identity and not Equality
     * to compare ids. So you cannot directly look for an equal but not identical key
     *
     * @param match a map that looks like the id we are looking for
     * @return the found tree id or null, if none
     */
    public findMatchingId(Map match) {
        tree.itemIds.find { id ->
            id instanceof Map && match.keySet().every { key ->
                id[key] == match[key]
            }
        }
    }
}

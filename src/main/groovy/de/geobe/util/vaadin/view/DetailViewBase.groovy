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

package de.geobe.util.vaadin.view

import de.geobe.util.statemachine.StateMachine
import de.geobe.util.vaadin.builder.SubTree
import de.geobe.util.vaadin.type.DetailSelector

/**
 * A base class for detail views controlled by a selection view like a Tree, TreeTable, List etc. .
 * The behaviour can be controlled efficiently by a state machine for detail views. 
 * There are two slightly different types of detail views:
 * <ol><li> TOPVIEW: Detail view(s) representing objects on the top level of an object hierarchy, 
 * usually on the root level of a tree view opr represented in a list view have their
 * create button active even if no top level element is selected directly or indirectly via a subelement</li>
 * <li> SUBVIEW: Detail views representing objects below the top level have their create button only active when an
 * object of a higher level is directly or indirectly selected so that the newly created object can be attached
 * to this higher level ("parent") element.</li></ol>
 * <p>The behaviour model assumes that the selection cannot change during edit or create, so the selection component
 * (Tree, List etc.) should be disabled during edit or create. Drawn as a state chart, 
 * the behaviour model looks like this:</p>
 * <pre>
 *
 *            SUBVIEW            TOPVIEW
 *               O                  O
 *               |                  |
 *             <init>             <init>
 *               |                  |
 *           +---v---+           +--v------+
 *           | INIT  |---root--->|  EMPTY  |
 *           +-v-----+           +|-^-v--^-+
 *             |                  | | |  |
 *             |    +---select----+ | |  |
 *        select    |  +----root----+ |  |
 *             |    |  |              |  |
 *     +----+  |    |  |         create cancel
 *    select|  |    |  |              |  |
 *     |  +-v--v----v--^-+       +----v--^-----+
 *     +-<|   SHOW       |<-save-| CREATEEMPTY |
 *        +v--^--^-v---v-+       +-------------+
 *         |  |  | |   +-------------------------+
 *         |  |  +-|-------cancel------+---------|------+
 *         |  +--|-|-------save----+---|---------|---+  |
 *         |  |  | +-------------+  |  |         |   |  |
 *         |  |  |               |  |  |         |   |  |
 *    create  |  |            edit  |  |    dialog   |  |
 *         |  |  |               |  |  |         |   |  |
 *        +v--^--^--+          +-v--^--^-+     +-v---^--^-+
 *        |  CREATE |          |  EDIT   |     |  DIALOG  |
 *        +---------+          +---------+     +----------+
 *
 * </pre>
 * @author georg beier
 */
abstract class DetailViewBase extends SubTree {
    protected StateMachine<DVState, DVEvent> sm
    protected DetailSelector detailSelector

    protected initSm(DVState initState) {

        sm = new StateMachine<>(initState)

        sm.addEntryAction(INIT, { clearFields(); initmode() })
        sm.addEntryAction(EMPTY, { emptymode() })
        sm.addEntryAction(SHOW, { showmode() })
        sm.addEntryAction(CREATEEMPTY, { createemptymode() })
        sm.addEntryAction(CREATE, { clearFields(); createmode() })
        sm.addEntryAction(EDIT, { editmode() })
        sm.addEntryAction(DIALOG, { dialogmode() })

        sm.addTransition(DVState.SUBVIEW, DVState.INIT, DVEvent.Init)
        sm.addTransition(DVState.INIT, DVState.SHOW, DVEvent.Select)
        sm.addTransition(DVState.INIT, DVState.EMPTY, DVEvent.Root)
        sm.addTransition(DVState.TOPVIEW, DVState.EMPTY, DVEvent.Init)
        sm.addTransition(DVState.EMPTY, DVState.EMPTY, DVEvent.Root)
        sm.addTransition(DVState.EMPTY, DVState.CREATEEMPTY, DVEvent.Create)
        sm.addTransition(DVState.EMPTY, DVState.SHOW, DVEvent.Select)
        sm.addTransition(DVState.CREATEEMPTY, DVState.SHOW, DVEvent.Save) {
            saveItem(0)
            setFieldValues()
            detailSelector.onEditItemDone(matchForNewItem, currentCaption, true)
        }
        sm.addTransition(DVState.CREATEEMPTY, DVState.EMPTY, DVEvent.Cancel) {
            detailSelector.onEditItemDone('', '')
        }
        sm.addTransition(DVState.SHOW, DVState.EDIT, DVEvent.Edit)
        sm.addTransition(DVState.SHOW, DVState.CREATE, DVEvent.Create)
        sm.addTransition(DVState.SHOW, DVState.SHOW, DVEvent.Select)
        sm.addTransition(DVState.SHOW, DVState.EMPTY, DVEvent.Root)
        sm.addTransition(DVState.EDIT, DVState.SHOW, DVEvent.Save) {
            saveItem(currentDomainId)
            detailSelector.onEditItemDone(currentItemId, currentCaption)
        }
        sm.addTransition(DVState.EDIT, DVState.SHOW, DVEvent.Cancel) {
            setFieldValues()
            detailSelector.onEditItemDone(currentItemId, currentCaption)
        }
        sm.addTransition(DVState.CREATE, DVState.SHOW, DVEvent.Save) {
            saveItem(0)
            detailSelector.onEditItemDone(matchForNewItem, currentCaption, true)
        }
        sm.addTransition(DVState.CREATE, DVState.SHOW, DVEvent.Cancel) {
            setFieldValues()
            detailSelector.onEditItemDone(currentItemId, currentCaption)
        }
        sm.addTransition(DVState.SHOW, DVState.DIALOG, DVEvent.Dialog)
        sm.addTransition(DVState.DIALOG, DVState.SHOW, DVEvent.Save) {
            saveDialog()
        }
        sm.addTransition(DVState.DIALOG, DVState.SHOW, DVEvent.Cancel) {
            cancelDialog()
        }
    }

    /** item id of currently selected object from vaadin selection component */
    protected abstract getCurrentItemId()
    /** value for the domain object id of currently displayed object */
    protected abstract Long getCurrentDomainId()
    /** get caption of current object for display in selection component */
    protected abstract String getCurrentCaption()
    /** item match mimics id for searching item in vaadin selection */
    protected abstract getMatchForNewItem()

    /** prepare for editing in CREATEEMPTY state */
    protected createemptymode() { editmode() }
    /** prepare for editing in CREATE state */
    protected createmode() { editmode() }
    /** prepare for working in DIALOG state */
    protected dialogmode() {}
    /** leaving DIALOG state with save */
    protected saveDialog() {}
    /** leaving DIALOG state with cancel */
    protected cancelDialog() {}
    /** prepare for editing in EDIT state */
    protected abstract editmode()
    /** prepare INIT state */
    protected initmode() {}
    /** prepare EMPTY state */
    protected abstract emptymode()
    /** prepare SHOW state */
    protected abstract showmode()
    /** clear all editable fields */
    protected abstract clearFields()
    /**
     * for the given persistent object id, fetch the full dto and save it in field currentDto
     * @param itemId object id
     */
    protected abstract void initItem(Long itemId)
    /**
     * set all fields from the current full dto object
     */
    protected abstract void setFieldValues()

    protected abstract saveItem(Long id)
}

enum DVState {
    SUBVIEW,        // creation state for detail views of sublevel objects
    TOPVIEW,        // creation state for detail views of toplevel objects
    INIT,           // nothing is selected in the controlling tree
    EMPTY,          // no object is selected for this tab, but a root node is selected
    SHOW,           // an object is selected and shown on the tab
    CREATEEMPTY,    // starting from EMPTY (important for Cancel events!), a new Object is created
    CREATE,         // starting from SHOW (important for Cancel events!), a new Object is created
    EDIT,           // selected object is being edited
    DIALOG,         // we are in a modal dialog
}

enum DVEvent {
    Init,     // initialise state machine
    Select,   // an item of the displayed class was selected
    Root,     // a new branch was selected, either by selecting another top level object or some subobject
    Edit,     // start editing the selected object
    Create,   // start creating a new object
    Cancel,   // cancel edit or create
    Save,     // save newly edited or created object
    Dialog,   // enter a modal dialog
}

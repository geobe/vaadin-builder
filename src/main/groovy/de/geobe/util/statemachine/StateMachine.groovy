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

package de.geobe.util.statemachine

import groovy.util.logging.Slf4j

/**
 * Implementation of a state machine with these features
 * <ul><li>States as enums,</li>
 * <li>Events as enums,</li>
 * <li>Transitions are triggered by a combination of current state and event,</li>
 * <li>onExit actions executed when leaving a state,</li>
 * <li>internalActions triggered by an event without state change.</li>
 * <li>onEntry actions executed when entering a state,</li>
 * <li>transitions actions executing after leaving a state and before entering a following state.
 * They can return a target state changing the following state.</li></ul>
 * @author georg beier
 * @param S enumertion Type for States
 * @param E enumeration type for events
 */
@Slf4j
class StateMachine<S extends Enum, E extends Enum> {
    /** map for internal actions, indexed by combination of currentState and event */
    private Map<Integer, SmAction> stateMachine = new HashMap<>()
    /** map for next states, indexed by combination of currentState and event */
    private Map<Integer, S> nextState = new HashMap<>()
    /** map of closures as entry actions */
    private Map<S, SmAction> onEntry = new HashMap<>()
    /** map of closures as exit actions */
    private Map<S, SmAction> onExit = new HashMap<>()
    /** Actions on a transition */
    private Map<TxKey, SmAction> transitions = new HashMap<>()
    /** index map of SxEs */
    private Map<Integer, TxKey> txMap = new HashMap<>()
    /** store current state */
    private S currentState
    /** for logging info to identify state machine instance */
    private String smid

    public Map<S, SmAction> getOnEntry() { onEntry }

    public Map<S, SmAction> getOnExit() { onExit }

    public HashMap<TxKey, SmAction> getTransitions() { transitions }

    def getCurrentState() { currentState }

    def setSmId(String id) { smid = id }

    /**
     * create instance with initial fromState
     * @param start initial fromState
     * @param id identifying string for logging and debug
     */
    StateMachine(S start, String id = 'default') {
        currentState = start
        smid = id
    }

    private buildIndex() {
        transitions.keySet().each { TxKey key ->
            txMap[key.trix()] = key
        }
    }

    /**
     * execute Action that is identified by currentState and event.
     * After execution, statemachine will be
     * <ul><li>in the following state as defined in addTransition method,
     * if closure returns no object of type S</li>
     * <li>in the state returned by the closure.</li>
     * <li>If no following state is defined, statemachine will stay in currentState and
     * no exitAction should have been executed.</li></ul>
     * @param event triggering event
     * @param params optional parameter to Action.act.
     *        Caution, Action.act will receive an Object[] Array
     * @return the current state after execution
     */
    S execute(E event, Object... params) {
        def index = trix(currentState, event)
        if (!txMap)
            buildIndex()
        TxKey txKey = txMap[index]
        if (txKey) {
            if(txKey.target) {
                // full transition
                def clex = onExit[txKey.start]
                clex?.call()
                def clac = transitions[txKey]
                def result = clac?.call(params)
                def next
                if (result instanceof S) {
                    next = result
                } else {
                    next = txKey.target
                }
                def clen = onEntry[next]
                clen?.call()
                log.info("Transition $smid: $currentState--$event->$next")
                currentState = next
            } else {
                // inner transition
                def clac = transitions[txKey]
                clac?.call()
                log.info("Inner Transition $smid: $currentState--$event->$currentState")
            }
        } else {
            log.info("ignored event $event in fromState $currentState")
        }
        currentState
    }

    /**
     * calculate a unique transition trix from current state and triggering event
     * @param st current state
     * @param ev event triggering transition
     * @return a unique Integer computed from state and event
     */
    public static Integer trix(S st, E ev) {
        def t = st.ordinal() + (ev.ordinal() << 12)
        t
    }

    /**
     * This SAM type (a type which defines a single abstract method) is used to
     * define state machine actions. It can be implemented directly by a Closure.
     */
    interface SmAction {
        S act(Object... params)
    }

    static class TxKey<S, E> implements Comparable<TxKey> {
        S start
        S target
        E trigger

        TxKey(S st, S tg, E tr) {
            start = st
            target = tg
            trigger = tr
        }

        @Override
        int compareTo(TxKey o) {
            return trix(start, trigger).compareTo(trix(o.start, o.trigger))
        }

        def trix() { trix(start, trigger) }
    }
}


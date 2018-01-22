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
 * furnished to do so, subject to the following conditionstart:
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
import spock.lang.Specification


/**
 * Created by georg beier on 22.01.2018.
 */
@Slf4j
class StateMachineSpecification extends Specification {

    static enum State {
        START, S1, S2, S3, END
    }

    static enum Event {
        Start, Intern, E1, E2, Exit
    }

    StateMachine<State, Event> sm

    def setup() {
        sm = new StateMachine<>(State.START, 'smtest')
        def enter = {
            log.info("enter ${sm.currentState}")
        }
        def exit = { log.info("leaving ${sm.currentState}") }
        def transarg = {Object... args -> log.info "on Transition with args: ${args[0]}"}
        def trans = { log.info "transitioning from ${sm.currentState}" }
        def transchange = {log.info 'transitioning to S3'; State.S3}
        def transintern = {log.info 'internal transition'}

        sm.onEntry[State.S1] = enter
        sm.onEntry[State.S2] = enter
        sm.onEntry[State.S3] = enter
        sm.onEntry[State.END] = enter

        def action = {log.info('Starting')}
        sm.transitions[new StateMachine<State, Event>.TxKey(State.START, State.S1, Event.Start)] = action
        sm.transitions[new StateMachine.TxKey(State.S1, State.S2, Event.E1)] = trans
        sm.transitions[new StateMachine.TxKey(State.S2, State.S1, Event.E1)] = trans
        sm.transitions[new StateMachine.TxKey(State.S1, null, Event.Intern)] = transintern
        sm.transitions[new StateMachine.TxKey(State.S1, State.S2, Event.E2)] = transarg
        sm.transitions[new StateMachine.TxKey(State.S2, State.S1, Event.E2)] = transchange
        sm.transitions[new StateMachine.TxKey(State.S3, State.S3, Event.E1)] = {log.info 'looping'}
        sm.transitions[new StateMachine.TxKey(State.S3, State.S3, Event.E2)] = {log.info 'ending'; State.END}
        sm.transitions[new StateMachine.TxKey(State.END, State.S3, Event.E2)] = {...args ->
            log.info 'let\'s restart'; args[0]
        }

        sm.onExit[State.S1] = exit
        sm.onExit[State.S2] = exit
        sm.onExit[State.S3] = exit
        sm.onExit[State.START] = exit

    }

    def 'the state machine should perform nicely' () {
        when: 'sm is created'
        then: 'it should be in Stert state'
        assert sm.currentState == State.START
        when: 'sm is started'
        sm.execute(Event.Start)
        then: 'sm is in S1'
        assert sm.currentState == State.S1
        when: 'E1 in S1'
        sm.execute(Event.E1)
        then: 'sm in S2'
        assert sm.currentState == State.S2
        when: 'E1 in S2'
        sm.execute(Event.E1)
        then: 'sm in S1'
        assert sm.currentState == State.S1
        when: 'Intern in S1'
        sm.execute(Event.Intern)
        then: 'sm in S1'
        assert sm.currentState == State.S1
        when: 'E2 with args in S1'
        sm.execute(Event.E2, 'Hello args', 'and much more...')
        then: 'sm in S2'
        assert sm.currentState == State.S2
        when: 'E2 in S2 with change'
        sm.execute(Event.E2)
        then: 'sm in S3'
        assert sm.currentState == State.S3
        when: 'E1 in S3 looping'
        sm.execute(Event.E1)
        then: 'sm in S3'
        assert sm.currentState == State.S3
        when: 'E2 in S3 with external change'
        sm.execute(Event.E2)
        then: 'sm in END'
        assert sm.currentState == State.END
        when: 'E2 in END with restart arg'
        sm.execute(Event.E2, State.START)
        then: 'sm in START'
        assert sm.currentState == State.START
    }
}

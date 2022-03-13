package com.searchicton.util;

/** Represents a function that has no parameters and a void return type. Used for callbacks in MapActivity */
@FunctionalInterface
public interface Action {

    /** Invoke the action */
    void invoke();

    /** Returns an action after that runs another action after this one */
    default Action andThenRun(Action after){
        return () -> {
            this.invoke();
            after.invoke();
        };
    }
}

package de.muenchen.oss.swim.dispatcher.domain.model;

/**
 * Available actions while dispatching.
 * Determines how the file is processed.
 */
public enum DispatchActions {
    /**
     * Dispatch file event to handler services.
     */
    DISPATCH,
    /**
     * Reroute file to another use case.
     */
    REROUTE,
    /**
     * File is marked as finished.
     * Alias for {@link #IGNORE}
     */
    DELETE,
    /**
     * File is marked as finished.
     * Alias for {@link #DELETE}
     */
    IGNORE
}

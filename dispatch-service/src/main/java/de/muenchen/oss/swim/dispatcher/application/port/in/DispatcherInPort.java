package de.muenchen.oss.swim.dispatcher.application.port.in;

public interface DispatcherInPort {
    /**
     * Trigger dispatching of files.
     * Loops over all configured use cases and triggers dispatching for matching files.
     */
    void triggerDispatching();
}

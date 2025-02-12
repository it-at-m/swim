package de.muenchen.oss.swim.dispatcher.application.port.in;

public interface ProtocolProcessingInPort {
    /**
     * Trigger processing of protocol files.
     * Loops over all configured use cases, loads, validates and processes matching protocol files.
     */
    void triggerProtocolProcessing();
}

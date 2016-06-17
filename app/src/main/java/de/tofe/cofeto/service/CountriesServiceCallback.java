package de.tofe.cofeto.service;

import java.nio.channels.Channel;

/**
 * Created by Felix on 17.06.2016.
 */
public interface CountriesServiceCallback {
    void serviceSuccess(Channel channel);
    void serviceFailure(Exception exception);
}

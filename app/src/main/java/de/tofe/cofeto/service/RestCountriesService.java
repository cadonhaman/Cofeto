package de.tofe.cofeto.service;

/**
 * Created by Felix on 17.06.2016.
 */
public class RestCountriesService {
    private CountriesServiceCallback callback;
    private String country;
    private Exception error;

    public RestCountriesService(CountriesServiceCallback callback) {
        this.callback = callback;
    }
}

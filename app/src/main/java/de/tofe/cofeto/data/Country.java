package de.tofe.cofeto.data;

import org.json.JSONObject;

/**
 * Created by Felix on 17.06.2016.
 */
public class Country implements JSONPopulator{
    private String capital;

    public String getCapital() {
        return capital;
    }

    @Override
    public void populate(JSONObject data) {
        capital = data.optString("capital");
    }
}

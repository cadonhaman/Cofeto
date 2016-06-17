package de.tofe.cofeto;

import android.app.Activity;
import android.app.ProgressDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

import java.nio.channels.Channel;

import de.tofe.cofeto.service.CountriesServiceCallback;
import de.tofe.cofeto.service.RestCountriesService;

public class MainActivity extends Activity implements CountriesServiceCallback {

    private TextView countriesTextView;

    private RestCountriesService service;

    private ProgressDialog dialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        countriesTextView = (TextView)findViewById(R.id.ergebnisFeld);

        service = new RestCountriesService(this);
        dialog = new ProgressDialog(this);
        dialog.setMessage("Laden...");
        dialog.show();

    }

    @Override
    public void serviceSuccess(Channel channel) {
        dialog.hide();
    }

    @Override
    public void serviceFailure(Exception exception) {
        dialog.hide();
        Toast.makeText(this, exception.getMessage(), Toast.LENGTH_LONG).show();
    }
}

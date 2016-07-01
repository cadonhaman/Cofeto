package de.tofe.cofeto;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.api.client.http.GenericUrl;
import com.google.api.client.http.HttpRequest;
import com.google.api.client.http.HttpRequestFactory;
import com.google.api.client.http.HttpResponse;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public class MainActivity extends Activity {

    public static final String TAG4LOGGING = "Cofeto";

    //Button mit dem der Web-Request gestartet wird
    protected Button _searchButton = null;

    //Eingabefeld für die Suche
    protected EditText _countryEditText = null;

    //Eingabe-String
    protected String _inputCountry = "";

    //TextView zur Anzeige des Ergebnisses des Web-Requests auch zur Anzeige von Fehlermeldungen
    protected TextView _resultTextView = null;

    //ImageView zur Anzeige der Flagge des entsprechenden Landes
    protected ImageView _flagImageView = null;

    //Kürzel für die Anzeige der Flagge
    protected String _countryCode = "";


    //Lifecycle-Methode; Layout für UI laden und Referenzen auf UI-Elemente holen
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        _searchButton       = (Button)   findViewById( R.id.searchButton );
        _countryEditText     = (EditText) findViewById( R.id.countryEditText );
        _resultTextView = (TextView) findViewById( R.id.resultTextView );
        _flagImageView    = (ImageView)findViewById( R.id.flagImageView);

        //Vertikales Scrolling ermöglichen
        _resultTextView.setMovementMethod(new ScrollingMovementMethod());

        //Listener für Tastatur-searchButton
        _countryEditText.setOnEditorActionListener(new TextView.OnEditorActionListener() {

            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                boolean handled = false;
                if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                    onSearch(null);
                    handled = true;
                }
                return handled;
            }
        });
    }

    //Event-Handler für Search-Button, wird in Layout-Daten mit Attribut "android:onClick" zugewiesen
    public void onSearch(View view) {

        //Tastatur nach Suche und Cursor ausblenden
        InputMethodManager mgr = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        mgr.hideSoftInputFromWindow(_countryEditText.getWindowToken(), 0);
        mgr.hideSoftInputFromWindow(this.getCurrentFocus().getWindowToken(),InputMethodManager.HIDE_NOT_ALWAYS);

        //Cursor deaktivieren während ein HTTP-Request läuft
        _countryEditText.setCursorVisible(false);

        //Button deaktivieren während ein HTTP-Request läuft
        _searchButton.setEnabled(false);

        //Input abspeichern
        _inputCountry = _countryEditText.getText().toString();

        //Hintergrund-Thread mit HTTP-Request starten
        MyBackgroundThread mbt = new MyBackgroundThread();
        mbt.start();
        _countryEditText.setCursorVisible(true);
    }

    //Überprüfung der Internetverbindung → Anzeige spezifischer Warnmeldung möglich
    public boolean isOnline() {
        ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo netInfo = cm.getActiveNetworkInfo();
        if (netInfo != null && netInfo.isConnectedOrConnecting()) {
            return true;
        }
        return false;
    }

    //Methoden für das Laden der Landesflagge
    public InputStream getFlag(String countryCode) throws Exception {
        //Zusammenbauen der URL für die Flagge zum eingegebenen Land
        URL url = new URL("http://www.geonames.org/flags/x/" + countryCode + ".gif");
        Log.i(TAG4LOGGING, "URL: " + url);

        HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
        Log.i(TAG4LOGGING, "Response Code of HTTP Request: " + urlConnection.getResponseCode() + urlConnection.getResponseMessage());

        return new BufferedInputStream(urlConnection.getInputStream());
    }

    protected void displayFlag(InputStream inputStream) throws IOException {
        final Bitmap bitmap = BitmapFactory.decodeStream( inputStream );

        inputStream.close();

        Log.i(TAG4LOGGING, "Bitmap dekodiert, Höhe=" + bitmap.getHeight() + ", Breite=" + bitmap.getWidth() );

        _flagImageView.post( new Runnable() {
            @Override
            public void run() {
                _flagImageView.setVisibility(View.VISIBLE);
                _flagImageView.setImageBitmap(bitmap);
            }
        });
    }

    //Methode für HTTP-Request zur Web-API
    protected String getDataFromWebAPI() throws Exception {

        //1: Request-Factory holen
        HttpTransport      httpTransport  = new NetHttpTransport();
        HttpRequestFactory requestFactory = httpTransport.createRequestFactory();

        //2: URL erzeugen und ggf. URL-Parametern hinzufügen
        GenericUrl url = new GenericUrl("https://restcountries.eu/rest/v1/name/" + _inputCountry + "?fullText=true");

        //3: eigentliches Absetzen des Requests
        HttpRequest  request      = requestFactory.buildGetRequest(url);
        HttpResponse httpResponse = request.execute();

        //4: Antwort-String (JSON-Format) zurückgeben
        String jsonResponseString = httpResponse.parseAsString();

        Log.i(TAG4LOGGING, "JSON-String erhalten: " + jsonResponseString);

        return jsonResponseString;
    }

    //Parsen des JSON-Dokuments, das von der Web-API zurückgeliefert wurde
    protected String parseJSON(String jsonString) throws Exception {

        if (jsonString == null || jsonString.trim().length() == 0) {
            return "Keine Informationen gefunden.";
        }

        JSONArray jsonArray = new JSONArray(jsonString); // eigentliches Parsen

        String capital = "";
        String region = "";
        String subregion = "";
        String population = "";
        String currencies = "";
        String borders = "";
        String callingCodes = "";
        String altSpellings = "";
        Character firstCharacter = null;
        Character secondCharacter = null;

        for (int i = 0; i < jsonArray.length(); i++) {
            JSONObject jsonobject = jsonArray.getJSONObject(i);

            //Informationen von JSONObject als String speichern
            capital = jsonobject.getString("capital");
            region = jsonobject.getString("region");
            subregion = jsonobject.getString("subregion");
            population = jsonobject.getString("population");
            currencies = jsonobject.getString("currencies");
            borders = jsonobject.getString("borders");
            callingCodes = jsonobject.getString("callingCodes");

            //Array-String verschönern
            currencies = currencies.replace("[", "");
            currencies = currencies.replace("]", "");
            callingCodes = callingCodes.replace("[", "");
            callingCodes = callingCodes.replace("]", "");
            borders = borders.replace("[", "");
            borders = borders.replace("]", "");
            currencies = currencies.replace("\"", "");
            borders = borders.replace("\"", "");
            callingCodes = callingCodes.replace("\"", "");
            currencies = currencies.replace(",", ", ");
            borders = borders.replace(",", ", ");
            callingCodes = callingCodes.replace(",", ", ");

            //Kürzel für Flaggen-Url herausziehen und übergeben
            altSpellings = jsonobject.getString("altSpellings");
            firstCharacter = altSpellings.charAt(2);
            secondCharacter = altSpellings.charAt(3);
            altSpellings = firstCharacter.toString() + secondCharacter.toString();
            altSpellings = altSpellings.toLowerCase();
            _countryCode = altSpellings;
        }

        //String für Ausgabe auf UI zusammensetzen
        return "Capital: " + capital  + "\nRegion: " + region  + "\nSubregion: " + subregion  + "\nPopulation: " + population   + "\nCurrencies: " + currencies    + "\nBorders: " + borders     + "\nCalling Codes: " + callingCodes;
    }


	/* *************************** */
	/* *** Start innere Klasse *** */
	/* *************************** */

    //Zugriff auf Web-API in eigenem Thread ausgelagert
    protected class MyBackgroundThread extends Thread {

        //Der Inhalt in der überschriebenen Methode wird in einem Hintergrund-Thread ausgeführt
        @Override
        public void run() {
            try {
                String jsonDocument ="";
                if(isOnline()) {
                    jsonDocument = getDataFromWebAPI();

                    String resString = parseJSON(jsonDocument);

                    displayResult( resString );

                    InputStream input = getFlag(_countryCode);

                    displayFlag(input);
                }
                else{
                    displayResult("Keine Internetverbindung verfügbar.");
                    flagReset();
                }
            }
            catch (Exception ex) {
                    displayResult("Keine Informationen gefunden.");
                    flagReset();
            }
        }

        //Methode um Ergebnis-String in TextView darzustellen
        protected void displayResult(String ergebnisStr) {

            final String finalString = ergebnisStr;

            _searchButton.post( new Runnable() {
                @Override
                public void run() {

                    _searchButton.setEnabled(true);

                    _resultTextView.setText(finalString);
                }
            });

        }

        //Zurücksetzen des Bildes
        protected void flagReset(){
            _searchButton.post( new Runnable() {
                @Override
                public void run() {

                    _flagImageView.setVisibility(View.GONE);
                }
            });
        }

    };

	/* *************************** */
	/* *** Ende innere Klasse  *** */
	/* *************************** */

}

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
    protected Button _suchButton = null;

    //Eingabefeld für die Suche
    protected EditText _landEditText = null;

    //Eingabe-String
    protected String _eingabeLand = "";

    //TextView zur Anzeige des Ergebnisses des Web-Requests auch zur Anzeige von Fehlermeldungen
    protected TextView _ergebnisTextView = null;

    protected ImageView _flagImageView = null;

    protected String _landkuerzel = "";


    //Lifecycle-Methode; Layout für UI laden und Referenzen auf UI-Elemente holen
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        _suchButton       = (Button)   findViewById( R.id.suchButton );
        _landEditText     = (EditText) findViewById( R.id.landEditText );
        _ergebnisTextView = (TextView) findViewById( R.id.ergebnisTextView );
        _flagImageView = (ImageView) findViewById( R.id.flagImageView);

        _ergebnisTextView.setMovementMethod(new ScrollingMovementMethod()); // um vertikales Scrolling zu ermöglichen

        //Listener für Tastatur-Suchbutton
        _landEditText.setOnEditorActionListener(new TextView.OnEditorActionListener() {

            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                boolean handled = false;
                if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                    onStartButtonBetaetigt(null);
                    handled = true;
                }
                return handled;
            }
        });
    }

    //Event-Handler für Start-Button, wird in Layout-Daten mit Attribut "android:onClick" zugewiesen
    public void onStartButtonBetaetigt(View view) {

        //Tastatur nach Suche und Cursor ausblenden
        InputMethodManager mgr = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        mgr.hideSoftInputFromWindow(_landEditText.getWindowToken(), 0);
        mgr.hideSoftInputFromWindow(this.getCurrentFocus().getWindowToken(),InputMethodManager.HIDE_NOT_ALWAYS);

        _landEditText.setCursorVisible(false);


        _suchButton.setEnabled(false); // Button deaktivieren während ein HTTP-Request läuft

        _eingabeLand = _landEditText.getText().toString();

        //Hintergrund-Thread mit HTTP-Request starten
        MeinHintergrundThread mht = new MeinHintergrundThread();
        mht.start();
        _landEditText.setCursorVisible(true);
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

    public InputStream holeBild(String landKuerzel)throws Exception {

        //Zusammenbauen der URL für die Flagge zum eingegebenen Land
        URL url = new URL("http://www.geonames.org/flags/x/" + landKuerzel + ".gif");
        Log.i(TAG4LOGGING, "URL: " + url);

        HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
        Log.i(TAG4LOGGING, "Response Code of HTTP Request: " + urlConnection.getResponseCode() + urlConnection.getResponseMessage());

        return new BufferedInputStream(urlConnection.getInputStream());
    }

    //Methode für HTTP-Request zur Web-API
    protected String holeDatenVonWebAPI() throws Exception {

        //1: Request-Factory holen
        HttpTransport      httpTransport  = new NetHttpTransport();
        HttpRequestFactory requestFactory = httpTransport.createRequestFactory();

        //2: URL erzeugen und ggf. URL-Parametern hinzufügen
        GenericUrl url = new GenericUrl("https://restcountries.eu/rest/v1/name/" + _eingabeLand + "?fullText=true");

        //3: eigentliches Absetzen des Requests
        HttpRequest  request      = requestFactory.buildGetRequest(url);
        HttpResponse httpResponse = request.execute();

        //4: Antwort-String (JSON-Format) zurückgeben
        String jsonResponseString = httpResponse.parseAsString();

        Log.i(TAG4LOGGING, "JSON-String erhalten: " + jsonResponseString);

        return jsonResponseString;
    }

    protected void bildDarstellen(InputStream inputStream) throws IOException {

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

    //Parsen des JSON-Dokuments <i>jsonString</i>, das von der Web-API zurückgeliefert wurde
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

            //Kürzel für Flagge-Url herausziehen und übergeben
            altSpellings = jsonobject.getString("altSpellings");
            firstCharacter = altSpellings.charAt(2);
            secondCharacter = altSpellings.charAt(3);
            altSpellings = firstCharacter.toString() + secondCharacter.toString();
            altSpellings = altSpellings.toLowerCase();
            _landkuerzel = altSpellings;
        }

        //String für Ausgabe auf UI zusammenbauen
        return "Capital: " + capital  + "\nRegion: " + region  + "\nSubregion: " + subregion  + "\nPopulation: " + population   + "\nCurrencies: " + currencies    + "\nBorders: " + borders     + "\nCalling Codes: " + callingCodes;
    }


	/* *************************** */
	/* *** Start innere Klasse *** */
	/* *************************** */

    //Zugriff auf Web-API (Internet-Zugriff) wird in eigenen Thread ausgelagert
    protected class MeinHintergrundThread extends Thread {

        //Der Inhalt in der überschriebenen <i>run()</i>-Methode wird in einem Hintergrund-Thread ausgeführt
        @Override
        public void run() {
            try {
                String jsonDocument ="";
                if(isOnline()) {
                    jsonDocument = holeDatenVonWebAPI();

                    String ergString = parseJSON(jsonDocument);

                    ergebnisDarstellen( ergString );

                    InputStream input = holeBild(_landkuerzel);

                    bildDarstellen(input);
                }
                else{
                    ergebnisDarstellen("Keine Internetverbindung verfügbar.");
                    bildReset();
                }
            }
            catch (Exception ex) {
                    ergebnisDarstellen("Keine Informationen gefunden.");
                    bildReset();
            }
        }

        //Methode um Ergebnis-String in TextView darzustellen
        protected void ergebnisDarstellen(String ergebnisStr) {

            final String finalString = ergebnisStr;

            _suchButton.post( new Runnable() { // wir könnten auch die post()-Methode des TextView-Elements verwenden
                @Override
                public void run() {

                    _suchButton.setEnabled(true);

                    _ergebnisTextView.setText(finalString);
                }
            });

        }

        protected void bildReset(){
            _suchButton.post( new Runnable() { // wir könnten auch die post()-Methode des TextView-Elements verwenden
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

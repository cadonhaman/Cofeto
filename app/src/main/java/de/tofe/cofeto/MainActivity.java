package de.tofe.cofeto;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.google.api.client.http.GenericUrl;
import com.google.api.client.http.HttpRequest;
import com.google.api.client.http.HttpRequestFactory;
import com.google.api.client.http.HttpResponse;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;

import org.json.JSONArray;
import org.json.JSONObject;

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


    //Lifecycle-Methode; Layout für UI laden und Referenzen auf UI-Elemente holen
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        _suchButton       = (Button)   findViewById( R.id.suchButton );
        _landEditText     = (EditText) findViewById( R.id.landEditText );
        _ergebnisTextView = (TextView) findViewById( R.id.ergebnisTextView );

        _ergebnisTextView.setMovementMethod(new ScrollingMovementMethod()); // um vertikales Scrolling zu ermöglichen

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

    //Tastatur nach Suche ausblenden
    /*InputMethodManager mgr = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
    mgr.hideSoftInputFromWindow(curEditText.getWindowToken(), 0);
    mgr.hideSoftInputFromWindow(this.getCurrentFocus().getWindowToken(),InputMethodManager.HIDE_NOT_ALWAYS);*/

    //Listener für Tastatur-Suchbutton


    //Event-Handler für Start-Button, wird in Layout-Daten mit Attribut "android:onClick" zugewiesen
    public void onStartButtonBetaetigt(View view) {

        // //ist nur für Tastatur
        InputMethodManager mgr = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        mgr.hideSoftInputFromWindow(_landEditText.getWindowToken(), 0);
        mgr.hideSoftInputFromWindow(this.getCurrentFocus().getWindowToken(),InputMethodManager.HIDE_NOT_ALWAYS);

        _landEditText.setCursorVisible(false);

        _suchButton.setEnabled(false); // Button deaktivieren während ein HTTP-Request läuft

        _eingabeLand = _landEditText.getText().toString();

        // Hintergrund-Thread mit HTTP-Request starten
        MeinHintergrundThread mht = new MeinHintergrundThread();
        mht.start();
    }


    //In dieser Methode wird der HTTP-Request zur Web-API durchgeführt
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


    //Parsen des JSON-Dokuments <i>jsonString</i>, das von der Web-API zurückgeliefert wurde
    protected String parseJSON(String jsonString) throws Exception {

        if (jsonString == null || jsonString.trim().length() == 0) {
            return "Keine Informationen gefunden.";
        }

        JSONArray jsonArray = new JSONArray(jsonString); // eigentliches Parsen

        String capital = "";
        String subregion = "";

        for (int i = 0; i < jsonArray.length(); i++) {
            JSONObject jsonobject = jsonArray.getJSONObject(i);
            capital = jsonobject.getString("capital");
            subregion = jsonobject.getString("subregion");
        }

        // String für Ausgabe auf UI zusammenbauen
        return "Hauptstadt: " + capital  + "\nRegion: " + subregion;
    }


	/* *************************** */
	/* *** Start innere Klasse *** */
	/* *************************** */

    // Zugriff auf Web-API (Internet-Zugriff) wird in eigenen Thread ausgelagert
    protected class MeinHintergrundThread extends Thread {

        //Der Inhalt in der überschriebenen <i>run()</i>-Methode wird in einem Hintergrund-Thread ausgeführt
        @Override
        public void run() {

            try {
                String jsonDocument = holeDatenVonWebAPI();

                String ergString = parseJSON(jsonDocument);

                ergbnisDarstellen( ergString );
            }
            catch (Exception ex) {
                ergbnisDarstellen( "Keine Informationen gefunden.");
                //ex.getMessage()
            }
        }


        //Methode um Ergebnis-String in TextView darzustellen
        protected void ergbnisDarstellen(String ergebnisStr) {

            final String finalString = ergebnisStr;

            _suchButton.post( new Runnable() { // wir könnten auch die post()-Methode des TextView-Elements verwenden
                @Override
                public void run() {

                    _suchButton.setEnabled(true);

                    _ergebnisTextView.setText(finalString);
                }
            });

        }

    };

	/* *************************** */
	/* *** Ende innere Klasse  *** */
	/* *************************** */

}

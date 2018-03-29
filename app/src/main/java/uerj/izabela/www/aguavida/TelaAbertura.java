package uerj.izabela.www.aguavida;


import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;

public class TelaAbertura extends Activity {

    // Timer da splash screen private
    // Timer da splash screen private
    static int SPLASH_TIME_OUT = 3000; //3000 melisegundos ou 3 segundos
    int i;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_start);

        new Handler().postDelayed(new Runnable() {
         /* * Exibindo splash com um timer. */

                                      @Override
                                      public void run() {
                                          // Esse método será executado sempre que o timer acabar // E inicia a activity principal
                                          Intent i = new Intent(TelaAbertura.this, MainActivity.class);
                                          startActivity(i);

                                          // Fecha esta activity
                                          finish();
                                      }
                                  },
                SPLASH_TIME_OUT);
    }
}
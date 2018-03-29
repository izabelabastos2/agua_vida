package uerj.izabela.www.aguavida;


import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.UUID;

public class MainActivity extends AppCompatActivity {

/////////////////////////////////////Declaração de variáveis/////////////////////////////////////////////////////

    // objetos do layout
   TextView descricao, telaSerial;


    static TextView statusConexao;

    //como podemos ter várias solicitações na tela
    //então criamos para cada tipo de solicitação um código diferente
    //para quando chegar o evento de volta sabermos qual informação queremos pegar
    private static final int SOLICITA_ATIVACAO_BT = 1;
    private static final int SOLICITA_CONEXAO = 2;

    //variável que controla se existe uma conexão em andamento
    boolean conexao = false;

    // Representa o local do dispositivo bluetooth.
    BluetoothAdapter meuBluetoothAdapter = null;
    BluetoothDevice meuDevice = null;
    //responsável por fazer a transição de dados da conexão
    BluetoothSocket meuSocket = null;
    OutputStream mmOutputStream;
    InputStream mmInputStream;
    Thread workerThread;
    byte[] readBuffer;
    int readBufferPosition;
    int counter;
    volatile boolean stopWorker;

    UUID meuUUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

    //variável que será usada para armazenar o endereço MAC do dispositivo escolhido na classe Conection
    private static String MAC;

    //ThreadConnectBTdevice myThreadConnectBTdevice;
    ThreadConnected myThreadConnected;

    //variáveis que controlam a trhead de contagem referente ao tempo que o arduino fica ligado
    Handler handlerDelay = new Handler();
    long delay = 90000; // tempo de delay em millisegundos

    //Definição da string que guarda o nome do arquivo onde fica salvo o ultimo endereço MAC selecionado
    public static final String ARDUINO_ADRESS = "arduinoAdress";


///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
//////////////////////////////////////////////FIM DECLARAÇÃO DE VARIÁVEIS GLOBAIS /////////////////////////////////////////////////////////////////////
//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        meuBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        //obtem o adptador bluetooth do aparelho
        if (meuBluetoothAdapter == null) {
            Toast.makeText(this, "Dispositivo não possui Bluetooth", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        else if(!meuBluetoothAdapter.isEnabled()){
            //intent que abre a tela padrão do aparelho para fazer a solicitação do bluetooth
            Intent ativaBluetooth = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            //como estamos enviando uma solicitação para tela, esperamos um resultado,
            //e esse resultado é gravado através de um inteiro, que definimos no início da classe
            //se recebido operação ocorreu adequadamente
            startActivityForResult(ativaBluetooth, SOLICITA_ATIVACAO_BT);
        }
        else{
            handlerDelay.postDelayed(new Runnable() {
                public void run() {
                    try {
                        setup();
                    } catch (Exception e) {

                    }
                }
            },800);
        }


        //evita, que pelo uso do handler, de algum erro na interface gráfica
        //handlerDelay.postDelayed(atualizaStatus, 0);
        //o segundo parâmetro representa o tempo de inicialização do handler.
        // Com o parâmetro " 0 " ele será inicializado imediatamente ao executar a aplicação


        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        getSupportActionBar().setDisplayShowHomeEnabled(true);
        getSupportActionBar().setIcon(R.drawable.menu_logo);


        telaSerial = (TextView) findViewById(R.id.telaSerial);

        

    }

///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
//////////////////////////////////////////////FIM ONCREATE DA CLASSE /////////////////////////////////////////////////////////////////////////////////
//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    //método usado para que se o usuária disser ok para abilitar o bluetooth
    //aplicação continua execução normalmente, e se disser não, a aplicação
    //é encerrada.
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        //super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode){
            case SOLICITA_ATIVACAO_BT:
                if(resultCode == Activity.RESULT_OK){
                    Toast.makeText(this, "Bluetooth Ativado com sucesso", Toast.LENGTH_SHORT).show();
                    setup();
                } else{
                    Toast.makeText(this, "Bluetooth desativo, O app será encerrado", Toast.LENGTH_SHORT).show();
                    finish();
                }
                break;
            case SOLICITA_CONEXAO:
                if(resultCode == Activity.RESULT_OK){
                    MAC = data.getExtras().getString(ListaDispositivos.ENDERECO_MAC);
                    Toast.makeText(this, MAC, Toast.LENGTH_SHORT).show();
                    Toast.makeText(this, "Realizando Conexão com Dispositivo: " + MAC , Toast.LENGTH_SHORT).show();
                    setup(); //setup(MAC);

                }else{
                    Toast.makeText(this, "Falha ao obter o Mac ", Toast.LENGTH_LONG).show();
                }
        }
    }



    private void setup()
    {

        // telaSerial.setText("Realizando Conexão...");
        Toast.makeText(this, "Realizando Conexão...", Toast.LENGTH_LONG).show();
        SharedPreferences prefs = getSharedPreferences(ARDUINO_ADRESS, MODE_PRIVATE);
        String restoredText = prefs.getString("text", null);
        String MACBluetooth = prefs.getString("mac", "No name defined");//"No name defined" is the default value.
        int idName = prefs.getInt("idmac", 0); //0 is the default value.

        meuDevice = meuBluetoothAdapter.getRemoteDevice(MACBluetooth);

        try{
            //criar canal do rfcomm
            meuSocket = meuDevice.createInsecureRfcommSocketToServiceRecord(meuUUID);
            meuSocket.connect();

            conexao = true;
            Toast.makeText(this, "Conexão realizada com sucesso ", Toast.LENGTH_LONG).show();
            myThreadConnected = new ThreadConnected(meuSocket);
            myThreadConnected.start();
        }catch (IOException erro){
            Toast.makeText(this, "Ocorreu um erro na conexão" + erro, Toast.LENGTH_LONG).show();
        }
    }

    /*
   ThreadConnected:
   Background Thread to handle Bluetooth data communication
   after connected
    */
    private class ThreadConnected extends Thread {
        private final BluetoothSocket connectedBluetoothSocket;
        private final InputStream connectedInputStream;
        private final OutputStream connectedOutputStream;


        public ThreadConnected(BluetoothSocket socket) {
            connectedBluetoothSocket = socket;
            InputStream in = null;
            OutputStream out = null;

            try {
                in = socket.getInputStream();
                out = socket.getOutputStream();
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }

            connectedInputStream = in;
            connectedOutputStream = out;
        }

        @Override
        public void run() {
            byte[] buffer = new byte[1024];
            int bytes;

            while (true) {
                try {
                    bytes = connectedInputStream.read(buffer);
                    final String strReceived = new String(buffer, 0, bytes);
                    // final String strByteCnt = String.valueOf(bytes) + " bytes received.\n";

                    runOnUiThread(new Runnable(){

                        @Override
                        public void run() {
                            telaSerial.append("");
                            telaSerial.append(strReceived);
                            //  telaSerial.append(strByteCnt);
                        }});

                } catch (IOException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();

                    final String msgConnectionLost = "Conexão perdida:\n"
                            + e.getMessage();
                    runOnUiThread(new Runnable(){

                        @Override
                        public void run() {
                            telaSerial.setText(msgConnectionLost);
                        }});
                }
            }
        }

        public void write(byte[] buffer) {
            try {
                connectedOutputStream.write(buffer);
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }

        public void cancel() {
            try {
                connectedBluetoothSocket.close();
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
    }




///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
//////////////////////////////////////////////INÍCIO ONCREATE MENU /////////////////////////////////////////////////////////////////////////////////
//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////




    /////////////////////////////////////////////////////////ON CREATE  do actionBar/////////////////////////////////////////////////////////////////////
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        switch(item.getItemId()){

            case R.id.action_connect://
                //se já existir uma conexão em andamento, queremos que desconecte
                if(conexao){
                    //desconectar
                    try{
                        if(myThreadConnected!=null){
                            myThreadConnected.cancel();
                        }
                        meuSocket.close();
                        Toast.makeText(this, "Bluetooth desconectado", Toast.LENGTH_LONG).show();
                        conexao = false;
                    }catch (IOException erro){
                        Toast.makeText(this, "Ocorreu um erro:" + erro, Toast.LENGTH_LONG).show();
                    }
                    startActivityForResult(new Intent(this, ListaDispositivos.class), SOLICITA_CONEXAO);
                }
                //caso não exista nenhuma conexão em andamento vamos conectar
                else{
                    startActivityForResult(new Intent(this, ListaDispositivos.class), SOLICITA_CONEXAO);
                }
                return true;
        }

        return super.onOptionsItemSelected(item);
    }

}

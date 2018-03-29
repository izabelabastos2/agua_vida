package uerj.izabela.www.aguavida;

import android.app.ListActivity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

/**
 * Created by Izabela on 05/07/2016.
 */
public class ListaDispositivos extends ListActivity {


    private BluetoothAdapter meuBluetoothAdapter2 = null;
    static String ENDERECO_MAC = null;
    private ArrayAdapter<String> listaDispositivos;
    private View v;

    //Definição da string que guarda o nome do arquivo onde fica salvo o ultimo endereço MAC selecionado
    public static final String ARDUINO_ADRESS = "arduinoAdress";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // take an instance of BluetoothAdapter - Bluetooth radio
        meuBluetoothAdapter2 = BluetoothAdapter.getDefaultAdapter();
        listaDispositivos = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1);

        meuBluetoothAdapter2 = BluetoothAdapter.getDefaultAdapter();

        listaDispositivos.clear();
        meuBluetoothAdapter2.startDiscovery();
        setListAdapter(listaDispositivos);
        registerReceiver(bReceiver, new IntentFilter(BluetoothDevice.ACTION_FOUND));
    }

    final BroadcastReceiver bReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            // When discovery finds a device
            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                // Get the BluetoothDevice object from the Intent
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                // add the name and the MAC address of the object to the arrayAdapter
                listaDispositivos.add(device.getName() + "\n" + device.getAddress());
                listaDispositivos.notifyDataSetChanged();
            }
        }
    };

    //O evento onListItemClick é chamado por padrão do android, sempre
    //que clicamos em um item de uma lista



    @Override
    protected void onListItemClick(ListView l, View v, int position, long id) {
        super.onListItemClick(l, v, position, id);



        String informacaoGeral = ((TextView)v).getText().toString();
        //agora vamos mostrar essa informação
        // Toast.makeText(getApplicationContext(),"info:" + informacaoGeral, Toast.LENGTH_LONG).show();
        String endereco_MAC = informacaoGeral.substring(informacaoGeral.length()-17);
        //Toast.makeText(getApplicationContext(), "mac:" + endereco_MAC, Toast.LENGTH_LONG).show();
        //agora que capturamos os dados precisamos retorná-los para a activity principal
        //vamos fazer isso através do intent
        Intent retornaMac = new Intent(); //cria intent

        // Salvando o endereço MAC do dispositivo selecionado no sistema
        SharedPreferences.Editor editor = getSharedPreferences(ARDUINO_ADRESS, MODE_PRIVATE).edit();
        editor.putString("mac", endereco_MAC);
        editor.commit();

        //vamos armazenar essa informação em uma variável,
        //no caso pegamos o valor da variavel endereco_MAC e colocamos
        //no ENDERECO_MAC definido globalmente.
        //Como ENDERECO_MAC é publica, podemos acessar ela na ou activity e pegar os dados dela
        //no caso o Intent é uma ponte para transportá-la
        retornaMac.putExtra(ENDERECO_MAC, endereco_MAC);//Passa valores para intent

        setResult(RESULT_OK, retornaMac); // defini o resultado da Intent

        //Após referenciar o endereço MAC temos que fechar a lista
        //pois não queremos que ela fique aparecendo
        meuBluetoothAdapter2.cancelDiscovery();
        finish();


    }

    @Override
    protected void onDestroy() {
        // TODO Auto-generated method stub
        super.onDestroy();
        unregisterReceiver(bReceiver);
    }

}

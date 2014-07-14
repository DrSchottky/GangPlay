package com.hexsample.gangplay;

import android.media.MediaPlayer;
import android.net.wifi.WifiConfiguration;
import android.os.AsyncTask;
import android.os.Bundle;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.drawable.Drawable;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import ar.com.daidalos.afiledialog.FileChooserDialog;
import java.io.File;
import java.util.ArrayList;

import com.example.gangplay.R;
import com.hexsample.wifihotspotutils.ClientScanResult;
import com.hexsample.wifihotspotutils.WifiApManager;

public class ServerActivity extends BlankActivity implements View.OnClickListener{


	
	ProgressBar pb;
	WifiApManager wifiApManager;
	ArrayList<ClientScanResult> clients;
	ProgressDialog spinner;
	pwrOn p;	
	
	
	/**
	 * Controllo se l'HotSpot WiFi è attivo.
	 */
	@Override
	protected void onCreate(Bundle savedInstanceState) 
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_server);
		Button btnOpen=(Button)findViewById(R.id.btnOpen);
		ImageView imgWifi=(ImageView)findViewById(R.id.wifiStatus);
		btnOpen.setOnClickListener(this);
		imgWifi.setOnClickListener(this);
		wifiApManager = new WifiApManager(getApplicationContext());
		checkhotspotStatus();
		
	}
	
	
	
	/**
	 * Controllo se l'HotSpot WiFi è attivo.
	 */
	@Override
	protected void onRestart()
	{
		checkhotspotStatus();
		super.onRestart();
	}
	
	
	/**
	 * Controllo se l'HotSpot WiFi è attivo.
	 */
	@Override
	protected void onResume()
	{
		checkhotspotStatus();
		super.onResume();
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
	    return super.onCreateOptionsMenu(menu);
	}
	
	
	/**
	 * Associo una funzione agli ImageButton.
	 */
	@Override
	public void onClick(View v) {
	       switch(v.getId()) {
	           case R.id.btnOpen:
	        	   openFile();
	        	   break;
	           case R.id.wifiStatus:
	        	   changeHotSpotStatus();
	        	   break;
	        	   }
	       }
	
	/**
	 * Mostro un Dialog che mi permette di selezionare dalla memoria del dispositivo una traccia in formato MP3.
	 * Selezionato il brano mi appare la lista dei dispositivi connessi all'HotSpot (o un messaggio di errore in caso ne ce ne siano).
	 * Da qui posso selezionare uno o più device che a cui inviare il file selezionato.
	 */
	private void openFile()
	{
		FileChooserDialog dialog = new FileChooserDialog(this);
		dialog.addListener(new FileChooserDialog.OnFileSelectedListener() {
	         public void onFileSelected(Dialog source, final File file) {
	             source.hide();
	             
	            	MediaPlayer mp=new MediaPlayer();
	            	try
	            	{
	            		mp.setDataSource(file.getAbsolutePath());
	            	}
	            	catch(Exception e)
	            	{
	            		Toast.makeText(getApplicationContext(), "Errore, file non supportato!",Toast.LENGTH_LONG).show();
	            		Log.e("ServerActivity.openFile", e.toString());
	            		return;
	            	}
	            	mp.release();
	            	
	            		    			
	    			Thread t = new Thread(new Runnable() {
						
						@Override
						public void run() {
							clients = wifiApManager.getClientList(true);
						}
					});
	    			t.start();
	            	while(t.isAlive()){}
	            	
	            	if(clients.size() == 0)
	            	{
	            		Toast.makeText(getApplicationContext(), "Nessun client trovato nella rete!", Toast.LENGTH_LONG).show();
	            		return;
	            	}
	            		
	            	final ArrayList<String> items = new ArrayList<String>();
	            	for(ClientScanResult val : clients)
	            	{
	            		items.add(val.getIpAddr());
	            	}
	            	final CharSequence[] elements = items.toArray(new CharSequence[items.size()]);
	            	
	            	final ArrayList<CharSequence> selectedItems = new ArrayList<CharSequence>();
	            	AlertDialog.Builder builder = new AlertDialog.Builder(ServerActivity.this);
	            	builder.setTitle("Scegli uno o più client");
	            	builder.setMultiChoiceItems(elements, null, new DialogInterface.OnMultiChoiceClickListener() {

						@Override
						public void onClick(DialogInterface dialog, int which,
								boolean isChecked) {
							if (isChecked) {
			                       selectedItems.add(elements[which]);
			                   } else if (selectedItems.contains(which)) {
			                       selectedItems.remove(elements[Integer.valueOf(which)]);
			                   }
						}
	            	});
	            	
	            	
	            	builder.setNeutralButton("OK", new DialogInterface.OnClickListener() {
						
						@Override
						public void onClick(DialogInterface dialog, int which) {
							if(!selectedItems.isEmpty())
							{
								Intent launchPlayer=new Intent(getApplicationContext(),ServerPlayerActivity.class);
		    	            	launchPlayer.putExtra("filePath", file.getAbsolutePath());
		    	            	launchPlayer.putCharSequenceArrayListExtra("clients", selectedItems);
		    	            	startActivity(launchPlayer);
		    	            	selectedItems.clear();
		    	            	clients.clear();
		    	            	items.clear();
							}
							
						}
					});
	            	
	            	
	            	AlertDialog alert = builder.create();
	            	alert.show();
	            	
	         }
	         public void onFileSelected(Dialog source, File folder, String name) {
	             source.hide();
	         }
	     });
		dialog.setFilter(".*mp3|.*MP3");
	    dialog.show();
	}
	
	
	/**
	 * Controlla se l'HotSpot è attivo e gestisce l'ImageButton dello st
	 */
	private void checkhotspotStatus() //public
	{
		ImageView hotspotIcon=(ImageView)findViewById(R.id.wifiStatus);
		Button btnOpen=(Button)findViewById(R.id.btnOpen);
		TextView btnOpenLabel=(TextView)findViewById(R.id.btnOpenLabel);
		
		if(wifiApManager.isWifiApEnabled())
		{
			Drawable wifi_on=(Drawable)getResources().getDrawable(R.drawable.wifi_on);
			hotspotIcon.setImageDrawable(wifi_on);
			btnOpen.setEnabled(true);
			btnOpen.setVisibility(View.VISIBLE);
			btnOpenLabel.setVisibility(View.VISIBLE);
		}
		else
		{
			Drawable wifi_off=(Drawable)getResources().getDrawable(R.drawable.wifi_off);
			hotspotIcon.setImageDrawable(wifi_off);
			btnOpen.setEnabled(false);
			btnOpen.setVisibility(View.INVISIBLE);
			btnOpenLabel.setVisibility(View.INVISIBLE);
		}
	}
	
	/**
	 * Inverte lo stato di abilitazione dell'HotSpot.
	 */
	void changeHotSpotStatus()
	{
		ImageView hotspotIcon=(ImageView)findViewById(R.id.wifiStatus);
		Button btnOpen=(Button)findViewById(R.id.btnOpen);
		TextView btnOpenLabel=(TextView)findViewById(R.id.btnOpenLabel);
		if(wifiApManager.isWifiApEnabled())
		{
			wifiApManager.setWifiApEnabled(null, false);
			Drawable wifi_off=(Drawable)getResources().getDrawable(R.drawable.wifi_off);
			hotspotIcon.setImageDrawable(wifi_off);
			btnOpen.setEnabled(false);
			btnOpen.setVisibility(View.INVISIBLE);
			btnOpenLabel.setVisibility(View.INVISIBLE);
		}
		else
		{
			Drawable wifi_on=(Drawable)getResources().getDrawable(R.drawable.wifi_on);
			hotspotIcon.setImageDrawable(wifi_on);
			btnOpen.setVisibility(View.VISIBLE);
			btnOpenLabel.setVisibility(View.VISIBLE);
			btnOpen.setEnabled(true);
			p=new pwrOn();
			p.execute();
		}
		
	}
	
	
	class pwrOn extends AsyncTask<Void, Void, Void>
	{
		@Override
		/**
		 * Mostra uno spinner che notifica l'attivazione.
		 */
		protected void onPreExecute(){
			spinner = new ProgressDialog(ServerActivity.this);
			spinner.setMessage("Accendo l'hotspot...");
			spinner.setCanceledOnTouchOutside(false);
			spinner.show();
	    }
		
		@Override
		/**
		 * Nasconde lo spinner.
		 */
		protected void onPostExecute(Void a){
			spinner.dismiss();
			checkhotspotStatus();

	    }
		
		@Override
		/**
		 * Mostra un Toast che avverte che è stata usata la password di default per l'HotSpot.
		 */
		protected void onProgressUpdate(Void... a)
		{
			Toast.makeText(getApplicationContext(), "Password usata: defaultpassword", Toast.LENGTH_LONG).show();
		}
		
		@Override
		/**
		 * Recupera le impostazioni HotSpot salvate nelle Shared Preferences, crea un oggetto WifiConfiguration e attiva l'HotSpot con quella configurazione.
		 */
		protected Void doInBackground(Void... params) {
			SharedPreferences settings=getSharedPreferences("pref",0);
			WifiConfiguration wc=new WifiConfiguration();
			wc.SSID=settings.getString("SSID", "GangPlayAP");
			wc.status = WifiConfiguration.Status.DISABLED;
		    wc.priority = 40;
		    if(settings.getString("authtype", "wpa").equals("open"))
		    {
		    	wc.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.NONE);
			    wc.allowedProtocols.set(WifiConfiguration.Protocol.RSN);
			    wc.allowedProtocols.set(WifiConfiguration.Protocol.WPA);
			    wc.allowedAuthAlgorithms.clear();
			    wc.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.CCMP);
			    wc.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.TKIP);
			    wc.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.WEP40);
			    wc.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.WEP104);
			    wc.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.CCMP);
			    wc.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.TKIP);
		    }
		    else
		    {
		    	wc.allowedProtocols.set(WifiConfiguration.Protocol.RSN);
				wc.allowedProtocols.set(WifiConfiguration.Protocol.WPA);
				wc.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.WPA_PSK);
				wc.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.CCMP);
				wc.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.TKIP);
				wc.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.WEP40);
				wc.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.WEP104);
				wc.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.CCMP);
				wc.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.TKIP);
				String pwd=settings.getString("pwd", "");
				if(pwd.equals("")||pwd.length()<8)
				{
					wc.preSharedKey="defaultpassword";
					publishProgress();
				}
				else
				{
					wc.preSharedKey=pwd;
				}
		    }
			wifiApManager.setWifiApEnabled(wc, true);
			while(!wifiApManager.isWifiApEnabled()) {}
			return null;
		}
		
	}
}

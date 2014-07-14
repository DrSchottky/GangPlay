package com.hexsample.gangplay;

import java.io.File;

import com.example.gangplay.R;

import android.app.AlertDialog;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.AudioManager;
import android.media.MediaMetadataRetriever;
import android.media.MediaPlayer;
import android.net.wifi.WifiManager;
import android.net.wifi.WifiManager.WifiLock;
import android.os.Bundle;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import android.provider.Settings;


public class ClientActivity extends BlankActivity
{
	
	ProgressDialog spinner;
	File file;
	MediaPlayer mp = new MediaPlayer();
	WifiManager wifiManager;
	WifiLock lock;
	int sync_adjust = 50;  //milliseconds
	AudioManager audioManager;
	boolean running = true;
	Intent intent;
	boolean clientServiceIsRunning=false;
	
	
	/**
	 * Acquisisce il lock sul wifi, prepara l'Intent per il Service e controlla lo stato del WiFi.
	 */
	@Override
    public void onCreate(Bundle savedInstanceState) 
	{
            super.onCreate(savedInstanceState);
            setContentView(R.layout.activity_client);
            
            registerReceivers();
            
            wifiManager = (WifiManager) getSystemService(Context.WIFI_SERVICE);
            lock = wifiManager.createWifiLock(WifiManager.WIFI_MODE_FULL_HIGH_PERF, "LockTag");
            lock.acquire();
            
            audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);

			int timeout = 30;
            intent = new Intent(this, ClientNetComService.class);
            intent.putExtra("timeout", timeout);
            wifiStatus();
            
    }

    
    /**
	 * Fa l'infalte del menù.
	 */
	@Override
    public boolean onCreateOptionsMenu(Menu menu) 
    {
            MenuInflater mMenuInflater = getMenuInflater();
            mMenuInflater.inflate(R.menu.actbar, menu);
            return true;
    }
    
    
    /**
     * Rilascia il lock sul WiFi e il MediaPlayer.
     * Se è attivo il NetService lo termina.
     */
	@Override
    protected void onDestroy()
    {
    	if(lock.isHeld()) lock.release();
    	mp.release();
    	unregisterReceivers();
    	if(clientServiceIsRunning)
    	{
    		stopService(intent);
    	}
    	
    	super.onDestroy();
    }
    
    
    /**
     * Registra il BroadCast Receiver e, se il servizio Client non è in esecuzione, controlla lo stato del WiFi.
     */
    @Override
    protected void onRestart()
    {
    	super.onRestart();
    	//registerReceivers();
    	if(!clientServiceIsRunning)
    	{
    		wifiStatus();
    	}
    	
    }
    
    
    /**
     * Alla pressione del Back Button richiama il metodo per gestire l'uscita dall'Activity.
     */
    @Override
    public void onBackPressed() 
    {
        /*running = false;
        super.onBackPressed();
        overridePendingTransition(R.anim.push_right_in, R.anim.push_right_out);*/
    	askForExit();
    }
    
    /**
     * Mostra un Dialog di conferma che, in caso di risposta affermativa, termina l'Activity.
     */
    private void askForExit()  //protected
    {
    	AlertDialog.Builder alert = new AlertDialog.Builder(this);
    	alert.setTitle("Uscire?");
    	alert.setMessage("La riproduzione verrà interrotta");
    	alert.setCancelable(false);
    	alert.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
			
			@Override
			public void onClick(DialogInterface dialog, int which) {
				dialog.dismiss();
				running = false;
				finish();
				overridePendingTransition(R.anim.push_right_in, R.anim.push_right_out);
			}
		});
    	alert.setNegativeButton("Annulla", new DialogInterface.OnClickListener() {
			
			@Override
			public void onClick(DialogInterface dialog, int which) {
				
			}
		});
    	alert.show();
    }
    
    /**
     * Registra il BroadCast Receiver.
     */
    private void registerReceivers()  //public
    {
    	LocalBroadcastManager.getInstance(getApplicationContext()).registerReceiver(msgReceiver, new IntentFilter("ClientNetComService"));
    }
    
    /**
     * Deregistra il BroadCast Receiver.
     */
    private void unregisterReceivers() //public
    {
    	LocalBroadcastManager.getInstance(getApplicationContext()).unregisterReceiver(msgReceiver);
    }
    
    
    /**
     * Prepara l'oggetto MediaPlayer e chiama il metodo per estrarre Artwork e MetaData.
     */
    private void startMusic() //public
    {
    	try
    	{
    		mp.reset();
    		mp.setDataSource(file.getAbsolutePath());
    		mp.prepare();
    	}
    	catch(Exception e)
    	{
    		Toast.makeText(getApplicationContext(), "Il file non è riproducibile", Toast.LENGTH_SHORT).show();
    		Log.e("ClientActivity.startMusic",e.toString());
    		return;
    	}
    	setArtwork(file.getAbsolutePath());
    	setMetaData(file.getAbsolutePath());
    }
    
    /**
     * Lancia la riproduzione della traccia.
     */
    private void playMusic() //public
    {
    	mp.start();
    }
    
    /**
     * Mette in pausa la riproduzione della traccia.
     */
    private void pauseMusic() //public
    {
    	mp.pause();
    }
    
    /**
     * Estrae e mostra (se presente) l'Artwork contenuto all'interno del file multimediale.
     * @param filePath Percorso del file multimediale sul dispositivo.
     */
    private void setArtwork(String filePath) //public
	{
		MediaMetadataRetriever dataRetriver = new MediaMetadataRetriever();
		dataRetriver.setDataSource(filePath);
		byte artworkRaw[]=dataRetriver.getEmbeddedPicture();
		ImageView artwork=(ImageView)findViewById(R.id.clientArtwork);
		if(artworkRaw!=null)
		{
			Bitmap bmp=BitmapFactory.decodeByteArray(artworkRaw, 0, artworkRaw.length);
			artwork.setImageBitmap(bmp);
		}
	}
    
    /**
     * Estre e mostra (se presenti) i MetaDati (titolo, artista, album) relativi alla traccia.
     * @param filePath Percorso del file multimediale sul dispositivo.
     */
    private void setMetaData(String filePath) //public
    {
    	MediaMetadataRetriever mr=new MediaMetadataRetriever();
    	mr.setDataSource(filePath);
    	TextView title=(TextView)findViewById(R.id.songTitle);
    	TextView artist=(TextView)findViewById(R.id.songArtist);
    	TextView album=(TextView)findViewById(R.id.songAlbum);
    	title.setText(mr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE));
    	artist.setText(mr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST));
    	album.setText(mr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUM));
    	
    }
    
    
    /**
     * Sposta il seek (posizione) della traccia in riproduzione, aggiungendo un delay per compensare il ritardo dovuto al tempo di trasmissione.
     * @param syncmsg Posizione 
     */
	private void handleSync(String syncmsg)
	{
		String[] tokens = syncmsg.split(" ");
		int position = (Integer.parseInt(tokens[1])) + sync_adjust;
		mp.seekTo(position);
	}
	
	/**
	 * Regola il volume del dispositivo adattandolo al range di valori supportati.
	 * @param volmsg Livello volume
	 */
	private void handleVolume(String volmsg)
	{
		String[] tokens = volmsg.split(" ");
		int volPercent = (Integer.parseInt(tokens[1]));
		int maxVol=audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
		float volume=((float)maxVol/100F)*volPercent;
		audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, (int)volume, 0);
		
	}
	
	
	/**
	 * Controlla se il WiFi è acceso.
	 * Se è spento propone un dialog che rimanda alle impostazioni WiFi del dispositivo.
	 * Se è acceso lancia il Client Service e si mette in ricezione.
	 */
	private void wifiStatus() //public
	{
		
		if(!wifiManager.isWifiEnabled())
		{
			AlertDialog.Builder builder= new AlertDialog.Builder(this);

			builder.setTitle("Il WiFi è spento");
			builder.setMessage("Accenderlo?");
			builder.setCancelable(false);
			builder.setPositiveButton("Si", new DialogInterface.OnClickListener()
			{
				
				@Override
				public void onClick(DialogInterface arg0, int arg1) {
					startActivity(new Intent(Settings.ACTION_WIFI_SETTINGS));
					
				}
			});
			builder.setNegativeButton("No", new DialogInterface.OnClickListener() {
				
				@Override
				public void onClick(DialogInterface arg0, int arg1) 
				{
					finish();
					overridePendingTransition(R.anim.push_right_in, R.anim.push_right_out);
					return;		
				}
			});
			builder.show();
		}
		else
		{
            startService(intent);
            clientServiceIsRunning=true;
		}

	}
	@SuppressWarnings("deprecation")
	/**
	 * Crea una notifica contenete l'artwork e i metadati.
	 * Se viene premuta rimanda alla Activity.
	 */
	private void updateNotification()
	{
		TextView title=(TextView)findViewById(R.id.songTitle);
    	TextView artist=(TextView)findViewById(R.id.songArtist);
    	ImageView artwork=(ImageView)findViewById(R.id.clientArtwork);
    	artwork.buildDrawingCache();
    	Bitmap artworkbmap = artwork.getDrawingCache();
    	Bitmap artwork_icon=Bitmap.createScaledBitmap(artworkbmap, 120, 120, true);
		Intent notificationIntent = new Intent(this, ClientActivity.class);
		notificationIntent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
		PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, 0);
		NotificationManager mNotificationManager=(NotificationManager)getSystemService(Context.NOTIFICATION_SERVICE);
		NotificationCompat.Builder mNotifyBuilder=new NotificationCompat.Builder(this);
		mNotifyBuilder.setContentTitle(title.getText());
		mNotifyBuilder.setContentText(artist.getText());
		mNotifyBuilder.setSmallIcon(R.drawable.app_logo_notification_icon);
		mNotifyBuilder.setLargeIcon(artwork_icon);
		mNotifyBuilder.setContentIntent(pendingIntent).getNotification();
		mNotificationManager.notify(1,mNotifyBuilder.build());
		
	}
	
	
	
	private BroadcastReceiver msgReceiver = new BroadcastReceiver() {
	  @Override
	  /**
	   * Estrae gli eventuali messaggi/comandi in arrivo sul Broadcast Receiver.
	   */
	  public void onReceive(Context context, Intent intent) 
	  {
		  if(intent.hasExtra("msg")) 
		  {
			  MSG(intent.getStringExtra("msg"));
		  }
		  else if(intent.hasExtra("fileName"))
		  {
			  file = new File(intent.getStringExtra("fileName"));
		  }
		  else if(intent.hasExtra("control"))
		  {
			  CONTROL(intent.getStringExtra("control"));
		  }
	  }
	  
	  /**
	   * Gestisce le casistiche dei vari tipi di messaggi.
	   * @param stringExtra Contenuto del messaggio.
	   */
	  private void MSG(String stringExtra)
	  {
		  if(stringExtra == "waitForFileTransfer_Wait")
			{
				spinner = new ProgressDialog(ClientActivity.this);
				spinner.setMessage("In attesa di connessione...");
	    		spinner.setCanceledOnTouchOutside(true);
	    		spinner.setOnCancelListener(new DialogInterface.OnCancelListener()
	    		{
	    	          public void onCancel(DialogInterface dialog) 
	    	          {
	    	             running = false;
	    	             stopService(new Intent(ClientActivity.this, ClientNetComService.class));
	    	             spinner.dismiss();
	    	             finish();
	    	             overridePendingTransition(R.anim.push_right_in, R.anim.push_right_out);
	    	          }
	    	    });
	   		    spinner.show();
			}
			else if(stringExtra == "waitForFileTransfer_Accepted")
			{
				spinner.setMessage("Ricezione file in corso...");
				spinner.setCanceledOnTouchOutside(false);
			}
			else if(stringExtra == "waitForFileTransfer_Incomplete")
			{
				spinner.dismiss();
				Toast.makeText(getApplicationContext(), "Errore durante la ricezione file", Toast.LENGTH_LONG).show();
			}
			else if(stringExtra == "waitForFileTransfer_Timeout")
			{
				spinner.dismiss();
				Toast.makeText(getApplicationContext(), "Timeout ricezione file scaduto", Toast.LENGTH_SHORT).show();
				finish();
			}
			else if(stringExtra == "waitForFileTransfer_Complete")
			{
				spinner.dismiss();
				startMusic();
				updateNotification();
			}
	  }
	  
	  /**
	   * Gestisce le casistiche dei vari tipi di comandi.
	   * @param stringExtra Contenuto del comando.
	   */
	  private void CONTROL(String stringExtra)
	  {
		  if(stringExtra.contains("play"))       playMusic();
		  else if(stringExtra.contains("start")) playMusic();
		  else if(stringExtra.contains("pausa")) pauseMusic();
		  else if(stringExtra.contains("sync")) handleSync(stringExtra);
		  else if(stringExtra.contains("volume")) handleVolume(stringExtra);
		  else if(stringExtra.contains("finish")) finish();
	  }
	  
	};
	
	
}
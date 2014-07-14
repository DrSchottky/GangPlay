package com.hexsample.gangplay;


import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.Socket;
import java.util.ArrayList;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Point;
import android.graphics.drawable.Drawable;
import android.media.AudioManager;
import android.media.MediaMetadataRetriever;
import android.media.MediaPlayer;
import android.net.wifi.WifiManager;
import android.net.wifi.WifiManager.WifiLock;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.Display;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.view.MotionEvent;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.Toast;

import com.example.gangplay.R;
import com.hexsample.wifihotspotutils.WifiApManager;

public class ServerPlayerActivity extends BlankActivity implements View.OnClickListener,OnSeekBarChangeListener{
	
	MediaPlayer mp = new MediaPlayer();
	Drawable playIcon;
	Drawable pauseIcon;
	Button btnPlay;
	SeekBar seekBar;
	ProgressDialog pd;
	WifiApManager wifiApManager;
	WifiManager wifiManager;
	WifiLock wifiLock;
	ArrayList<String> clients;
	public Socket[] socks;
	String filePath;
	int screenHeight;
	AudioManager audioManager;
	TelephonyManager telephonyManager;
	int GLOBAL_TOUCH_POSITION_Y = 0;
	int GLOBAL_TOUCH_CURRENT_POSITION_Y = 0;
	int deltaVol = 0;
	int finVol = 0;
	Intent intent;
	
	/**
	 * Se si riceve una chiamata la riproduzione viene messa in pausa.
	 */
	private PhoneStateListener mPhoneListener = new PhoneStateListener() {
		  public void onCallStateChanged(int state, String incomingNumber) {
		   try {
		    if(state == TelephonyManager.CALL_STATE_RINGING) pauseMusic();
		   } catch (Exception e) {
		    Log.i("Exception", "PhoneStateListener() e = " + e);
		   }
		   super.onCallStateChanged(state, incomingNumber);
		  }
		 };
	
	
	
	/**
	 * Associa una azione al click del pulsante Play/Pausa.
	 */
	@Override
	   public void onClick(View v) {
	       switch(v.getId()) {
	           case R.id.btnPlay:
	        	   if(mp.isPlaying())
	        	   {
	        		   pauseMusic();
	        	   }
	        	   else
	        		{
	        		   playMusic();
	        		}
	        	   break;
	       	}
	       }
	
	
	
	/**
	 * Sposta il seek della traccia quando viene toccata la ProgressBar
	 */
	@Override
    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
		if(fromUser)
		{
			try
			{
				mp.seekTo(progress);
			}
			catch (Exception e)
			{
				Log.e("ServerPlayerActivity.onProgressChanged",e.toString());
			}
		}	
    }
	
	
	
	/**
	 * Al rilascio della ProgressBar invia la posizione ai client.
	 */
	@Override
    public void onStopTrackingTouch(SeekBar seekBar) {
		sendSync();
    	seekBar.setSecondaryProgress(seekBar.getProgress()); // set the shade of the previous value.	
    }
	
	@Override
    public void onStartTrackingTouch(SeekBar seekBar) 
	{
    }
	
	
	/**
	 * Fa Override del comando volume laterale, convertendolo il valore in percentuale a causa dei diversi range di valori supporatti dai dispositivi.
	 * Viene modificato il livello volume del server e di tutti i client connessi.
	 */
	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) 
	{ 
		 if(keyCode == KeyEvent.KEYCODE_VOLUME_UP || keyCode==KeyEvent.KEYCODE_VOLUME_DOWN)
		 {
			int maxVol=audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
			int currVol=audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)+1;
			float volPercent=((float)currVol/(float)maxVol)*100;
			sendVolume((int)volPercent);
			return super.onKeyDown(keyCode, event);
		 }	
		 else
			return super.onKeyDown(keyCode, event);
		
	}
	

	
	/**
	 * Recupera il path della traccia selezionata, inizializza l'AudioManager e acquisisce il lock sul WiFi.
	 * Dopodiché lancia l'AsyncTask per il trasferimento del file e prepara l'ambiente per la riproduzione
	 */
	@Override
    public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            setContentView(R.layout.activity_player);
            btnPlay=(Button)findViewById(R.id.btnPlay);
        	pauseIcon = getResources().getDrawable( R.drawable.btn_pause);
        	playIcon = getResources().getDrawable( R.drawable.btn_play);
        	seekBar=(SeekBar)findViewById(R.id.seekBar1);
        	seekBar.setOnSeekBarChangeListener(this); 
        	clients = new ArrayList<String>(getIntent().getExtras().getStringArrayList("clients"));
        	filePath = getIntent().getExtras().getString("filePath");
        	audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        	
        	
        	//Lock the WiFi to get the highest performance
        	wifiManager = (WifiManager) getSystemService(Context.WIFI_SERVICE);
        	wifiLock = wifiManager.createWifiLock(WifiManager.WIFI_MODE_FULL_HIGH_PERF, "LockTag");
        	wifiLock.acquire();
        	
        	//Async Task to do the file transfer
        	AsyncNetTransfer connection = new AsyncNetTransfer();
            connection.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, (Void[])null);
        	    
        	
            openFile(filePath);
            setArtwork(filePath);
            btnPlay.setOnClickListener(this);
            seekBar.setMax(mp.getDuration());
            Display screen = getWindowManager().getDefaultDisplay();
            Point size = new Point();
            screen.getSize(size);
            screenHeight = size.y;
            RelativeLayout area = (RelativeLayout) findViewById(R.id.screen);
            area.setOnTouchListener(
            		new RelativeLayout.OnTouchListener(){
            			
            			@Override
            			public boolean onTouch(View v,MotionEvent m)
            			{
            				handleTouch(m);
            				return true;
            			}
            		});
            
            registerIncomingCallReceiver();
            
            }

	/**
	 * Istanzia l'oggetto per gestire gli eventi telefonici.
	 */
    private void registerIncomingCallReceiver() {
    	telephonyManager = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
		telephonyManager.listen(mPhoneListener, PhoneStateListener.LISTEN_CALL_STATE);
	}
    


	
	/**
	 * Fa l'inflate del menu.
	 */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
            MenuInflater mMenuInflater = getMenuInflater();
            mMenuInflater.inflate(R.menu.actbar, menu);
            return true;
    }
    
    
    /**
     * Comunica ai client connessi di terminare la riproduzione.
     * Rilascia il MediaPlayer, il Wifi Lock e il listener telefonico.
     * Se il Service di rete è in esecuzione lo termina.
     */
    @Override
    protected void onDestroy()
    {
    	sendMessageToNetService("finish");
    	mp.release();
    	if(wifiLock.isHeld()) wifiLock.release();
    	telephonyManager.listen(mPhoneListener, PhoneStateListener.LISTEN_NONE);
    	if(intent!=null)
    	{
    		stopService(intent);
    	}
    	super.onDestroy();
    }
    
    
    
    /**
     * Chiama il metodo per gestire la terminazione delle operazioni.
     */
    @Override
	public void onBackPressed()
    {
    	askForExit();
    }

    
    /**
     * Mostra un Dialog che chiede la conferma dell'operazione.
     * Se viene confermata l'Activity viene terminata.
     */
    protected void askForExit()
    {
    	AlertDialog.Builder alert = new AlertDialog.Builder(this);
    	alert.setTitle("Uscire?");
    	alert.setMessage("La riproduzione verrà interrotta");
    	alert.setCancelable(false);
    	alert.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
			
			@Override
			public void onClick(DialogInterface dialog, int which) {
				dialog.dismiss();
				finish();
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
	 * Fa Override della gesture "two finger drag" per regolare il volume, convertendolo il valore in percentuale a causa dei diversi range di valori supporatti dai dispositivi.
	 * Viene modificato il livello volume del server e di tutti i client connessi.
	 */
    void handleTouch(MotionEvent m)
    {
    	if(m.getPointerCount()==2 && mp!=null)
    	{
    		int currVol = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
    		switch(m.getActionMasked())
    		{
    		case MotionEvent.ACTION_MOVE:
    			GLOBAL_TOUCH_CURRENT_POSITION_Y=(int)m.getRawY();
    			int diff=GLOBAL_TOUCH_POSITION_Y-GLOBAL_TOUCH_CURRENT_POSITION_Y;
    			deltaVol=(audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)*diff)/screenHeight;
    			finVol=currVol+deltaVol;
    			break;
    		case MotionEvent.ACTION_POINTER_DOWN:
    			GLOBAL_TOUCH_POSITION_Y=(int)m.getRawY();
    			break;
    		case MotionEvent.ACTION_POINTER_UP:
    			GLOBAL_TOUCH_POSITION_Y=(int)m.getRawY();
    			audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, finVol, 0);
    			int maxVol=audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
    			float volPercent=((float)finVol/(float)maxVol)*100;
    			sendVolume((int)volPercent);
    			finVol=0;
    			deltaVol=0;
    			break;
    		}
    	}
    	else
    	{
    		GLOBAL_TOUCH_POSITION_Y=0;
    		GLOBAL_TOUCH_CURRENT_POSITION_Y=0;
    	}
    }
    
    
    /**
     * Lancia il Servizio di rete allegandogli, tramite Intent, i client connessi.
     */
    private void launchNetService()
    {
    	intent = new Intent(this, ServerNetComService.class);
    	intent.putStringArrayListExtra("hosts", clients);
    	startService(intent);
    }
    
    
    /**
     * Inizializza il MediaPlayer col file selezionato e lancia un AsyncTask per la gestione della ProgressBar.
     * @param filePath Percorso del file sul dispositivo.
     */
    private void openFile(String filePath)
    {
    	try
    	{
    		mp.setDataSource(filePath);
    		mp.prepare();
    	}
    	catch (Exception e)
    	{
    		Toast.makeText(getApplicationContext(), "Il file non è riproducibile", Toast.LENGTH_SHORT).show();
    		Log.e("ServerPlayerActivity.openFile",e.toString());
    	}
    	AsyncPlayerSeekBar task = new AsyncPlayerSeekBar();
    	task.execute(mp);	
    }

    
	
	/**
	 * Inizia la riproduzione della traccia sul server e sui client connessi.
	 */
    @SuppressWarnings("deprecation")
	private void playMusic()
	{
    	mp.start();
    	while(!mp.isPlaying())
    	{
    		//Wait
    	}
    	sendMessageToNetService("play");
	    btnPlay.setBackgroundDrawable(pauseIcon);
	    
	}

	
	/**
	 * Interrompe la riproduzione della traccia sul server e sui client connessi.
	 */
	@SuppressWarnings("deprecation")
	private void pauseMusic()
	{
		mp.pause();
		while(mp.isPlaying())
		{
			//Wait
		}
		sendMessageToNetService("pause");
		onProgressChanged(seekBar, mp.getCurrentPosition(), true);
		onStopTrackingTouch(seekBar);
		btnPlay.setBackgroundDrawable(playIcon);
		
	}
	
	/**
	 * Invia ai client connessi il seek attuale della traccia.
	 */
	private void sendSync()
	{
		String posizione = "sync " + mp.getCurrentPosition();
		sendMessageToNetService(posizione);
	}
	
	/**
	 * Invia ai client connessi il volume da applicare.
	 * @param volume Valore del volume in percentuale.
	 */
	private void sendVolume(int volume)
	{
		String msg = "volume " + volume;
		sendMessageToNetService(msg);
	}
	
	
	/**
	 * Invia al Service di rete un messaggio.
	 * @param message Testo del messaggio.
	 */
	private void sendMessageToNetService(String message)
	{
		Intent intent = new Intent("ServerPlayerActivity");
		intent.putExtra("message", message);
		LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
	}
	
	
	/**
	 * Estrae dalla traccia e mostra l'Artwork associato (se presente).
	 * @param filePath Percorso della traccia sul dispositivo.
	 */
	private void setArtwork(String filePath)
	{
		MediaMetadataRetriever dataRetriver = new MediaMetadataRetriever();
		dataRetriver.setDataSource(filePath);
		byte artworkRaw[]=dataRetriver.getEmbeddedPicture();
		ImageView artwork=(ImageView)findViewById(R.id.artwork);
		if(artworkRaw!=null)
		{
			Bitmap bmp=BitmapFactory.decodeByteArray(artworkRaw, 0, artworkRaw.length); 
			artwork.setImageBitmap(bmp);
		}
	}
	
	
	
	
	
	class AsyncPlayerSeekBar extends AsyncTask<MediaPlayer, Void, Void> {
		
	    
	    /**
	     * Ogni secondo aggiorna la posizione della ProgressBar per sincronizzarla col seek della traccia.
	     * Al termine della riproduzione riporta la ProgressBar alla posizione iniziale.
	     */
		@Override
	    protected Void doInBackground(MediaPlayer... mp) {
	    	int currentPosition= 0;
	        int total = mp[0].getDuration();
	        while (mp!=null) {
	        	if(currentPosition<total)
	        	{
	            try {
	                Thread.sleep(1000);
	                currentPosition= mp[0].getCurrentPosition();
	                }  
	            catch (Exception e) {
	                return null;
	                }            
	            seekBar.setProgress(currentPosition);
	        	}
	        	else
	        	{
	        		mp[0].seekTo(0);
	        		currentPosition=0;
	        		seekBar.setProgress(currentPosition);
	        		publishProgress();
	                }
	        	}
	            return null;
	    }

	    
	    /**
	     * Mette in pausa la traccia.
	     */
		@Override
	    protected void onProgressUpdate(Void...a) {
	    	pauseMusic();
	    	
	    }

	    
	}
	
	
	
	
	class AsyncNetTransfer extends AsyncTask<Void, Integer, Void> {

		
		/**
		 * Mostra il Dialog del trasferimento del file.
		 */
		@Override
		protected void onPreExecute(){
		pd = new ProgressDialog(ServerPlayerActivity.this);
	    pd.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
	    pd.setMessage("Uploading File...");
	    pd.setCanceledOnTouchOutside(false);
	    pd.show();
	    pd.setProgressNumberFormat("%1d KB / %2d KB");
	    socks = new Socket[clients.size()];
	    }
		
		
		/**
		 * Controlla che tutti i client selezionati siano raggiungibili, altrimenti termina l'operazione.
		 */
		@Override
		protected void onPostExecute(Void a){
			for(Socket socket : socks)
			{
				if(socket != null)
				{
					if(!socket.isConnected()) 
					{
						Toast.makeText(getApplicationContext(), "Client non disponibile", Toast.LENGTH_SHORT).show();
						finish(); 
						return; 
					}
				}
				else 
				{ 
					Toast.makeText(getApplicationContext(), "Client non disponibile", Toast.LENGTH_SHORT).show();
					finish(); 
					return; 
				}
			}
			launchNetService();
	    }
		
		
		
		/**
		 * Per ogni client specificato apre sequenzialmente un socket e trasferisce il file.
		 * Ogni 8 byte trasferiti viene aggionato il Dialog che mostra il progresso del trasferimento.
		 */
		@Override
	    protected Void doInBackground(Void... a) {
			final int port = 8888;
			DataOutputStream dos = null;
			int count = 0;
			
			for(String client : clients)
			{
				Socket tmp_sock = null;
				publishProgress(-1);
				try
				{
					tmp_sock = new Socket(client, port);
					tmp_sock.setTcpNoDelay(true);
				}
				catch(Exception e)
				{
					Log.e("ServerPlayerActivity.doInBackground(General Exception)", e.toString());
					return null;
				}
				socks[count] = tmp_sock;
				++count;
			}
			
			for(Socket socket : socks)
			{
				
				File myFile = new File(filePath);
				long filesize = myFile.length();
				
			    byte[] mybytearray = new byte[8192];
			    DataInputStream dis = null;
				try 
				{
					dis = new DataInputStream(new BufferedInputStream(new FileInputStream(myFile)));
				} 
				catch (FileNotFoundException e) 
				{
					Log.e("ServerPlayerActivity.doInBackground(FileNotFoundException)",e.toString());
				}
			    try 
			    {
			        dos = new DataOutputStream(socket.getOutputStream());
			        dos.writeUTF(myFile.getName());
			        dos.writeLong(filesize);
			        int read;
			        int size = (int)filesize / 1024;
			        pd.setMax(size);
			        int counter = 0;
					while((read = dis.read(mybytearray)) != -1){
			            dos.write(mybytearray, 0, read);
			            counter += 8;
			            publishProgress((counter));
			        }
			    } 
			    catch (IOException e) 
			    {
			    	Log.e("ServerPlayerActivity.doInBackground(IOException)",e.toString());
			    }
			    pd.dismiss();
			    
			    
			    try 
			    {
					dis.close();
				} 
			    catch (IOException e) 
			    {
			    	Log.e("ServerPlayerActivity.doInBackground(IOException:dis.close())",e.toString());
				}
			}
		    
	            return null;
	    }

	    
	    /**
	     * Mostra sulla UI il progresso del trasferimento.
	     */
		@Override
	    protected void onProgressUpdate(Integer...a) {
	    	if(a[0] == -1) 
	    		{
	    			pd.show();
	    			pd.setProgressNumberFormat("%1d KB / %2d KB");
	    		}
	    	pd.setProgress(a[0]);
	    }
	    
	}
	
	
}
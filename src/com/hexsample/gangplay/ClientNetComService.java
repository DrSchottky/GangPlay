package com.hexsample.gangplay;

import java.io.DataInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;

import com.example.gangplay.R;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Environment;
import android.os.IBinder;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;


public class ClientNetComService extends Service {
	
	public netThread scThread;
	

	/**
	 * Al lancio del servizio faccio partire il Thread per gestire lo scambio file e le comunicazioni di rete.
	 * Creo una notifica per avvisare che il servizio è in esecuzione.
	 */
	@SuppressWarnings("deprecation")
	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		SharedPreferences settings = getSharedPreferences("pref", 0);
		int timeout=settings.getInt("value", 30);
		scThread = new netThread(timeout * 1000);
		scThread.start();
		Notification notification = new Notification(R.drawable.app_logo_notification_icon, "GangPlay", System.currentTimeMillis());
		Intent notificationIntent = new Intent(this, ClientActivity.class);
		notificationIntent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
		PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, 0);
		notification.setLatestEventInfo(this, "GangPlay","Client in esecuzione", pendingIntent);
		startForeground(1, notification);
	    return Service.START_NOT_STICKY;
	}
	
	@Override
	public void onCreate()
	{
		super.onCreate();
	}
	
	
	/**
	 * Interrompo il Thread e il Service in background.
	 */
	@Override
	public void onDestroy()
	{
		scThread.stopSelf();
		stopForeground(true);
		super.onDestroy();
	}
	
	
	/**
	 * Non applico nessun tipo di bind.
	 */
	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}
	
	
	
	public class netThread extends Thread
	{
		
		private Socket socket;
		
		private String filename;
		
		private int timeout;
		
		ServerSocket sSock;
		
		boolean isRunning = false;
		
		
		/**
		 * Gestisco il trasferimento del file e preparo il socket per ricevere comandi.
		 * Se le operazioni vanno a buon fine mi metto in ascolto di comandi da parte del server.
		 */
		@Override
		public void run()
		{
			this.isRunning = true;			
			if(waitForFileTransfer(timeout))
				netComLoop();
		}
		
		/**
		 * Termino il Thread, il Service e chiudo il socket.
		 */
		private void stopSelf() //public
		{
			this.isRunning = false;
			try {
				this.socket.close();
				this.sSock.close();
			} catch (Exception e) {
				Log.e("ClientNetComService.stopSelf",e.toString());
			}
			this.interrupt();
			try {
				sSock.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
			//this.interrupt();
		}
		
		/**
		 * Imposto il timout di ricezione del Client.
		 * @param timeout Valore del timeout (millisecondi).
		 */
		private netThread(int timeout) //public
		{
			this.timeout = timeout;
		}
		
		/**
		 * Apro un socket in ascolto e attendo che il server invii il file.
		 * Al completamento del trasferimento apro un socket per ricevere i comandi.
		 * Ad ogni step viene inviato un Intent per notificare successi o fallimenti.
		 * @param timeout Timeout apertura del socket in ricezione.
		 * @return {@code true} Se il trasferimento e l'apertura del socket in ascolto si concludono correttamente, {@code false} in caso di errori.
		 */
		private boolean waitForFileTransfer(int timeout) //public
		{
			
			try {
				sSock = new ServerSocket(8888);
				sSock.setSoTimeout(timeout);
				sendBroadcast("waitForFileTransfer_Wait");
				this.socket = sSock.accept();
			} 
			catch (SocketTimeoutException e)
			{
				Log.e("ClientNetComService.waitForFileTransfer",e.toString());
				sendBroadcast("waitForFileTransfer_Timeout");
				stopSelf();
				return false;
			}
			catch (IOException e) {
				e.printStackTrace();
			}
			if(!this.isRunning) return false;

			if(this.socket.isConnected()) 
			{			
				
				sendBroadcast("waitForFileTransfer_Accepted");
				
				try
				{
			    InputStream in;
			    int byteRead = 0;

					in = this.socket.getInputStream();
			        DataInputStream clientData = new DataInputStream(in);
			        this.filename = clientData.readUTF();
			        float dimension = clientData.readLong();
			        OutputStream output = new FileOutputStream(Environment.getExternalStorageDirectory()+"/Download/" + this.filename);
			        byte[] buffer = new byte[8192];
			        int read;
			        while(byteRead < dimension)
			        {
			        	read = clientData.read(buffer);
			            byteRead += read;
			            output.write(buffer, 0, read);
			            if(byteRead >= dimension) break;
			        }
			        output.close();
			        
			        float dimensione = ((float)dimension / 1024F) / 1024F;
			        dimensione = (float)Math.round(dimensione * 100) / 100;
			        File f = new File(Environment.getExternalStorageDirectory()+"/Download/" + this.filename);
			        if(f.length() != dimension)
			        {
			        	sendBroadcast("waitForFileTransfer_Incomplete");
			        	socket.close();
			        	stopSelf();
			        }
			    }
			catch(Exception e)
			{
				e.printStackTrace();
			}
			
				//Invio un Intent che notifica al UI-thread il file trasferito e il path del file
				sendBroadcast(Environment.getExternalStorageDirectory()+"/Download/" + this.filename, "fileName");
				sendBroadcast("waitForFileTransfer_Complete");
				try 
				{
					this.socket.close();
				} 
				catch (IOException e) 
				{
					e.printStackTrace();
				}
				
				waitForConnection();
			return true;
		}
			else
			{
				sendBroadcast("waitForFileTransfer_Timeout");
				stopSelf();
				return false;
			}
		

		}
		
		
		/**
		 * Apro un socket e mi metto in attesa di connessione da parte del client.
		 */
		private void waitForConnection() //public
		{
			try 
			{
				ServerSocket sSock = new ServerSocket(8889);
				this.socket = sSock.accept();
			} 
			catch (IOException e) 
			{
				Log.e("ClientNetComService.waitForConnection",e.toString());
			}
			
			if(this.socket.isConnected())
			{
				sendBroadcast("waitForConnection_Connected");
				return;
			}
			
		}
		
		
		/**
		 * Finché il socket è aperto ricevo e faccio il pasing dei messaggi.
		 */
		private void netComLoop() //public
		{
			try 
			{
					DataInputStream input = new DataInputStream(socket.getInputStream());
					while(!this.socket.isClosed())
					{
						if(input.available() == 0) continue;
						
						String line = input.readUTF();
						
						if(line == null || line == "") continue;
						
						Log.d("ClientNetComService.netComLoop", "Letto:"+line);
						
						if(line.contains("start"))  sendBroadcast("start", "control");
						else if(line.contains("play")) sendBroadcast("play", "control");
						else if(line.contains("pause")) sendBroadcast("pausa", "control");
						else if(line.contains("sync")) sendBroadcast(line, "control");
						else if(line.contains("volume")) sendBroadcast(line, "control");
						else if(line.contains("finish")) sendBroadcast("finish", "control");
					}
			}
			catch(Exception e)
			{
				Log.e("ClientNetComService.netComLoop", e.toString());
			}
		}
		
		
		
		/**
		 * Invia un messaggio come Intent Broadcast.
		 * @param message Testo del mesaggio.
		 */
		private void sendBroadcast(String message) //public
		{
			Intent intent = new Intent("ClientNetComService");
			intent.putExtra("msg", message);
			LocalBroadcastManager.getInstance(ClientNetComService.this).sendBroadcast(intent);
		}
		
		/**
		 * Invia un messaggio/comando come Intent Broadcast.
		 * @param message Testo del messaggio.
		 * @param name Tipo di messaggio.
		 */
		private void sendBroadcast(String message, String name)
		{
			Intent intent = new Intent("ClientNetComService");
			intent.putExtra(name, message);
			LocalBroadcastManager.getInstance(ClientNetComService.this).sendBroadcast(intent);
		}
		
	}
	
}

package com.hexsample.gangplay;

import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.ArrayList;

import com.example.gangplay.R;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.IBinder;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

public class ServerNetComService extends Service
{
	
	private ArrayList<String> hosts;
	
	private ServerNetThread netThread;

	
	/**
	 * Il Service non ha alcun Bind.
	 */
	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}
	

	/**
	 * Recupera l'elenco di host client, crea una Notification per avvisare dello stato del server e lancia il Thread di rete.
	 */
	@SuppressWarnings("deprecation")
	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		if(!intent.hasExtra("hosts")) stopSelf();
		this.hosts = intent.getStringArrayListExtra("hosts");
		netThread = new ServerNetThread();
		netThread.start();
		Notification notification = new Notification(R.drawable.app_logo_notification_icon, "GangPlay", System.currentTimeMillis());
		Intent notificationIntent = new Intent(this, ServerPlayerActivity.class);
		notificationIntent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
		PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, 0);
		notification.setLatestEventInfo(this, "GangPlay","Server in esecuzione", pendingIntent);
		startForeground(2, notification);
		return Service.START_NOT_STICKY;
	}
	
	@Override
	public void onDestroy() {
		netThread.stopSelf();
		super.onDestroy();
	}
	
	
		
	public class ServerNetThread extends Thread
	{
		private ArrayList<Socket> socket;
		
		
		/**
		 * Creo un Array di socket, gestisco le connessioni e registro il broadcast receiver.
		 */
		@Override
		public void run()
		{
			socket = new ArrayList<Socket>();
			socketConnect();
			registerReceiver();
		}
		
		/**
		 * Chiudo tutti i socket aperti e interrompo il Thread.
		 */
		private void stopSelf()
		{
			for(Socket sock : socket)
			{
				try 
				{
					sock.close();
				} 
				catch (IOException e) 
				{
					e.printStackTrace();
				}
			}
			unregisterReceiver();
			this.interrupt();
		}
		
		/**
		 * Per ogni host client creo un Socket che inserisco nell'array di Socket.
		 */
		private void socketConnect()
		{
			for(String host : hosts)
			{
				Socket temp_sock = new Socket();
				
					try
					{
						temp_sock.setTcpNoDelay(true);
						Thread.sleep(500);
						temp_sock.connect(new InetSocketAddress(host, 8889), 500);
						socket.add(temp_sock);

					}
					catch(Exception e)
					{
						Log.e("ServerNetComService.socketConnect", e.toString());
						e.printStackTrace();
					}
			}
		}
		
		/**
		 * Invia un messaggio a tramite tutti i socket aperti.
		 * @param message Testo del messaggio.
		 */
		private void sendMessageToAll(String message)
		{
			for(Socket val : socket)
			{
				try
				{
					DataOutputStream out = new DataOutputStream(val.getOutputStream());
					out.writeUTF(message);
					out.flush();
				} 
				catch (Exception e) 
				{
					Log.e("ServerNetComService.sendMessageToAll", e.toString());
				}
			}
		}
		
		/**
		 * Registra un Broadcast Receiver, filtrando i risultati tramite IntentFilter
		 */
		private void registerReceiver()
		{
			LocalBroadcastManager.getInstance(ServerNetComService.this).registerReceiver(msgReceiver, new IntentFilter("ServerPlayerActivity"));
		}
		
		/**
		 * Deregistra il Broadcast Receiver.
		 */
		private void unregisterReceiver()
		{
			LocalBroadcastManager.getInstance(ServerNetComService.this).unregisterReceiver(msgReceiver);
		}
		
		
		// handler for received filename from service
		private BroadcastReceiver msgReceiver = new BroadcastReceiver() {
		  @Override
		  public void onReceive(Context context, Intent intent) 
		  {
			  netThread.sendMessageToAll(intent.getStringExtra("message"));
		  }
		};
		
		
	}
	
}

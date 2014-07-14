package com.hexsample.gangplay;


import java.util.List;

import com.example.gangplay.R;

import android.annotation.SuppressLint;
import android.app.ActionBar;
import android.app.Activity;
import android.app.ActivityManager;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.NumberPicker;
import android.widget.RadioGroup;
import android.widget.TextView;

public class BlankActivity extends Activity{
	static SharedPreferences settings;
	SharedPreferences.Editor edit;
	
	
	/**
	 * Imposta la ActionBar e recupera le SharedPreferences.
	 */
	@SuppressLint("CommitPrefEdits")
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.timeout_dialog);
		ActionBar actBar= getActionBar();
		actBar.setDisplayShowTitleEnabled(false);
		setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
		settings=getSharedPreferences("pref",0);
		edit=settings.edit();
}

	
	/**
	 * Fa l'infalte del menù.
	 */
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
	    inflater.inflate(R.menu.actbar, menu);
	    return super.onCreateOptionsMenu(menu);
	}
	
	
	/**
	 * Gestisce il click sulle varie voci del menù e della ActionBar.
	 */
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		ActivityManager am = (ActivityManager) this.getSystemService(ACTIVITY_SERVICE);
        List< ActivityManager.RunningTaskInfo > taskInfo = am.getRunningTasks(1);
        String currentActivity=taskInfo.get(0).topActivity.getShortClassName();
        currentActivity=currentActivity.substring(currentActivity.lastIndexOf("."));
	    switch (item.getItemId()) {
	    case R.id.serverView:
	        if(currentActivity.equals(".ServerActivity") || currentActivity.equals(".ServerPlayerActivity") || currentActivity.equals(".ClientActivity") )
	        	return true;
	        
	    		        
	        Intent launchServer=new Intent(getApplicationContext(),ServerActivity.class);
	    	startActivity(launchServer);
	        overridePendingTransition(R.anim.right_in, R.anim.left_out);
	    	return true;
	    case R.id.clientView:
	    	if(currentActivity.equals(".ServerPlayerActivity") || currentActivity.equals(".ClientActivity"))
	    		return true;
	    	
	    	Intent launchClient=new Intent(getApplicationContext(),ClientActivity.class);
	    	startActivity(launchClient);
	        overridePendingTransition(R.anim.push_left_in, R.anim.push_left_out);
	    	return true;
	    case R.id.credits:
	    	AlertDialog.Builder builder= new AlertDialog.Builder(this);
	    	builder.setTitle("Credits:");
	    	builder.setMessage("Coded by \n\nSebastian Davrieux \nLuca Cirani");
	    	builder.setCancelable(true);
	    	builder.show();
	    	return true;
	    case R.id.clientTimeout:
	    	setTimeout();
	    	return true;
	    case R.id.hotspotConfig:
	    	setHotSpotConfig();
	    	return true;
	    	
	    	
	    default:
	    	return super.onOptionsItemSelected(item);
	    }
	}
	
	/**
	 * Crea e mostra il dialog per il timeout.
	 * Usa le SharedPreferences per recuperare il valore attuale e salva quello scelto sul NumberPicker.
	 */
	private void setTimeout()
	{	
		LayoutInflater factory=getLayoutInflater();
		View v=factory.inflate(R.layout.timeout_dialog, null);
		AlertDialog.Builder builder= new AlertDialog.Builder(this);
		final NumberPicker np=(NumberPicker)v.findViewById(R.id.numberPicker1);
		String[] values=new String[20];
		for(int i=0;i<20;i++)
		{
			values[i]=Integer.toString(10*(i+1));
		}
		np.setMinValue(0);
		np.setMaxValue(values.length-1);
		np.setValue((getTimeout()/10)-1);
		np.setDisplayedValues(values);
		
		builder.setCancelable(false);
		builder.setView(v);
		builder.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
			
			@Override
			public void onClick(DialogInterface arg0, int arg1) {
				saveTimout((np.getValue()+1)*10);
			}
		});
		builder.setNegativeButton("Annulla", new DialogInterface.OnClickListener() {
			
			@Override
			public void onClick(DialogInterface arg0, int arg1) {
				
				
			}
		});
		builder.show();
		
	}
	
	/**
	 * Salva il timout nelle SharedPreferences.
	 * @param value Valore (in secondi) del timeout
	 */
	private void saveTimout(int value)
	{	
		edit.putInt("value", value);
		edit.commit();
	}
	
	/**
	 * Preleva il timout salvato nelle SharedPreferences.
	 * @return Il valore salvato o, se inesistente, 30.
	 */
	static int getTimeout()
	{
		int result=settings.getInt("value", 30);
		return result;
	}
	
	
	/**
	 * Genera il Dialog che preleva dalle SharedPreferences la configurazione attuale per l'HotSpot.
	 * I cambiamenti effettuati verranno salvati nelle SharedPreferences se si preme su "Ok", ignorati se si preme su "Annulla".
	 */
	private void setHotSpotConfig()
	{
		LayoutInflater factory=getLayoutInflater();
		View v=factory.inflate(R.layout.hotspotconfig_dialog, null);
		AlertDialog.Builder builder= new AlertDialog.Builder(this);
		builder.setCancelable(false);
		builder.setView(v);
		final RadioGroup rg=(RadioGroup)v.findViewById(R.id.passwordTypeRadioGroup);
		final EditText pwd=(EditText)v.findViewById(R.id.passwordTextBox);
		final EditText SSID=(EditText)v.findViewById(R.id.SSIDTextBox);
		final TextView pwdLabel=(TextView)v.findViewById(R.id.passwordLabel);
		SSID.setText(settings.getString("SSID", "GangPlayAP"));
		pwd.setText(settings.getString("pwd", ""));
		if(settings.getString("authtype", "wpa").equals("open"))
		{
			rg.check(R.id.noPasswordRadio);
			pwd.setVisibility(View.INVISIBLE);
			pwd.setEnabled(false);
			pwdLabel.setVisibility(View.INVISIBLE);
			pwdLabel.setEnabled(false);
		}
		builder.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
			
			@Override
			public void onClick(DialogInterface arg0, int arg1) {
				if(SSID.getText().toString().equals(""))
				{
					edit.putString("SSID", "GangPlayAP");
				}
				else
				{
					edit.putString("SSID", SSID.getText().toString());
				}
			  
				
				switch(rg.getCheckedRadioButtonId())
				{
				case R.id.noPasswordRadio:
					edit.putString("authtype", "open");
					break;
				case R.id.WPAPasswordRadio:
					edit.putString("authtype", "wpa");
					edit.putString("pwd", pwd.getText().toString());
					break;
					
				}
				edit.commit();
			    
			}
		});
		
		builder.setNegativeButton("Annulla", new DialogInterface.OnClickListener() {
			
			@Override
			public void onClick(DialogInterface arg0, int arg1) {
				
			}
		});
		builder.show();
		rg.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
			
			@Override
			public void onCheckedChanged(RadioGroup group, int checkedId) {
				switch(checkedId)
				{
				case R.id.noPasswordRadio:
					pwd.setVisibility(View.INVISIBLE);
					pwd.setEnabled(false);
					pwdLabel.setVisibility(View.INVISIBLE);
					pwdLabel.setEnabled(false);
					break;
				case R.id.WPAPasswordRadio:
					pwd.setVisibility(View.VISIBLE);
					pwd.setEnabled(true);
					pwdLabel.setVisibility(View.VISIBLE);
					pwdLabel.setEnabled(true);
					break;
				}
				
			}
			
		});
	}

}

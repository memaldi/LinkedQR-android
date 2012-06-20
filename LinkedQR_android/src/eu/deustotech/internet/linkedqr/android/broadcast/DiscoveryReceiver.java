package eu.deustotech.internet.linkedqr.android.broadcast;

import android.bluetooth.BluetoothAdapter;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class DiscoveryReceiver extends BroadcastReceiver {

	@Override
	public void onReceive(Context context, Intent intent) {
		// TODO Auto-generated method stub
		
		  int mode = intent.getIntExtra(BluetoothAdapter.EXTRA_SCAN_MODE, BluetoothAdapter.ERROR);
		if (mode!= BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE )
		{
			Log.e("discovery", "apagado");
			Intent discoverableIntent = new
			Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
			discoverableIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 10);
			discoverableIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
			context.startActivity(discoverableIntent);
			
		}
		
		
		
		if (mode== BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE )
		{
			Log.e("discovery", "encendido");
		}
	}
	
	
	

}

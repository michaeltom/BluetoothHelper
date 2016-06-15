package net.flyget.bluetoothhelper;

import android.app.Activity;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;

public class AboutActivity extends Activity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		super.onCreate(savedInstanceState);
		setContentView(R.layout.about_layout);
		
		TextView tv = (TextView) findViewById(R.id.textView1);
		String text = String.format("\n\n Bluetooth Debug Helper for Android \n\n Version%s \n\n\n http://blog.csdn.net/mr_raptor \n\n tangpan09@gmail.com\n\n", getVersionName());
		tv.setText(text);
	}
	
	private String getVersionName(){
		 String versionName = "";  
		    try {  
		        // ---get the package info---  
		        PackageManager pm = this.getPackageManager();  
		        PackageInfo pi = pm.getPackageInfo(this.getPackageName(), 0);  
		        versionName = pi.versionName;  
		        if (versionName == null || versionName.length() <= 0) {  
		            return "";  
		        }  
		    } catch (Exception e) {  
		        Log.e("VersionInfo", "Exception", e);  
		    }  
		    return versionName;  
	}

	@Override
	protected void onDestroy() {
		// TODO Auto-generated method stub
		super.onDestroy();
	}
	
}

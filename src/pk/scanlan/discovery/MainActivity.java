package pk.scanlan.discovery;


import java.net.InetAddress;
import java.net.UnknownHostException;

import pk.scanlan.discovery.tools.Host;
import pk.scanlan.discovery.tools.ManagedReceiver;
import pk.scanlan.discovery.tools.System;

import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.app.Activity;
import android.app.ListActivity;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences.Editor;
import android.graphics.Typeface;
import android.text.Html;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.AdapterView.OnItemLongClickListener;

public class MainActivity  extends ListActivity {
	protected static final String TAG = null;
	TextView text;
	private HostAdapter  	 mTargetAdapter   	     = null;
	private Discovery mNetworkDiscovery		 = null;
	private HostReceiver mHostReceiver 		 = null;
	private HostAdapter mHostAdapter = null;
	private Toast 			 mToast 			 	 = null;
	private long  			 mLastBackPressTime 	 = 0;

	private class HostAdapter extends ArrayAdapter<Host> 
	{		
		class HostHolder
	    {
	        ImageView  itemImage;
	        TextView   itemTitle;
	        TextView   itemDescription;
	    }
		
		public HostAdapter(  ) {		
	        super( MainActivity.this, R.layout.host_list_item);	        
	    }

		@Override
		public int getCount(){
			return System.getHosts().size();
		}
		
		@Override
	    public View getView( int position, View convertView, ViewGroup parent ) {		
	        View 		 row    = convertView;
	        HostHolder holder = null;
	        
	        if( row == null )
	        {
	            LayoutInflater inflater = ( LayoutInflater )MainActivity.this.getSystemService( Context.LAYOUT_INFLATER_SERVICE );
	            row = inflater.inflate( R.layout.host_list_item , parent, false );
	            
	            holder = new HostHolder();
	            
	            holder.itemImage  	   = ( ImageView )row.findViewById( R.id.itemIcon );
	            holder.itemTitle  	   = ( TextView )row.findViewById( R.id.itemTitle );
	            holder.itemDescription = ( TextView )row.findViewById( R.id.itemDescription );

	            row.setTag(holder);
	        }
	        else
	        {
	            holder = ( HostHolder )row.getTag();
	        }
	        
	        Host host = System.getHost( position );
	        
	        if( host.hasAlias() == true )
	        {
	        	holder.itemTitle.setText
	        	(
	    			Html.fromHtml
		        	(
		        	  "<b>" + host.getAlias() + "</b> <small>( " + host.getAddress() +" )</small>"
		  			)	
	        	);
	        }
	        else
	        	holder.itemTitle.setText( host.getAlias() );
	        
        	holder.itemTitle.setTypeface( null, Typeface.NORMAL );
      //TODO  	holder.itemImage.setImageResource( host.getDrawableResourceId() );
        	holder.itemDescription.setText( host.getDescription() );
        		       	       	        
	        return row;
	    }
	}

	
	private class HostReceiver extends ManagedReceiver
	{
		private static final String TAG = "HostReceiver";
		private IntentFilter mFilter = null;
		
		public HostReceiver() {
			mFilter = new IntentFilter();
			
			mFilter.addAction( Discovery.NEW_HOST );
			mFilter.addAction( Discovery.HOST_UPDATE );
		}
		
		public IntentFilter getFilter( ) {
			return mFilter;
		}
		
		@Override
		public void onReceive( Context context, Intent intent ) 
		{
			if( intent.getAction().equals( Discovery.NEW_HOST ) )
			{
				String address  = ( String )intent.getExtras().get( Discovery.HOST_ADDRESS ),
					   hardware = ( String )intent.getExtras().get( Discovery.HOST_HARDWARE ),
					   name		= ( String )intent.getExtras().get( Discovery.HOST_NAME );
				final  Host host = new Host( address );
				Log.d(TAG,"odebran "+address+" "+hardware+" "+name );
				
				if( host != null)
				{
					if( name != null && name.isEmpty() == false )
						host.setAlias( name );
					
					host.setHardwerAddres( hardware );
																												
					// refresh the target listview
	            	MainActivity.this.runOnUiThread(new Runnable() {
	                    @Override
	                    public void run() {
	                    	if( System.addOrderedHost( host ) == true )
							{
	                    		mHostAdapter.notifyDataSetChanged();
							}
	                    }
	                });		
				}
			}	
			else if( intent.getAction().equals(Discovery.HOST_UPDATE ) )
			{
				// refresh the target listview
            	MainActivity.this.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                    	mHostAdapter.notifyDataSetChanged();
                    }
                });		
			}					
		}
	}
	

	public void createOnlineLayout( ) {
		mHostAdapter = new HostAdapter( );
		
		setListAdapter( mTargetAdapter );	
	
		getListView().setOnItemLongClickListener( new OnItemLongClickListener() 
		{
			@Override
			public boolean onItemLongClick( AdapterView<?> parent, View view, int position, long id ) {									
				
														
				return false;
			}
		});


		if( mHostReceiver == null )		
			mHostReceiver = new HostReceiver();
		
		
	
	    mHostReceiver.unregister();
	   
	    
	    mHostReceiver.register( MainActivity.this );		
 
        mHostAdapter = new HostAdapter();
		setListAdapter(mHostAdapter);
	startNetworkDiscovery(false);
	
	}
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		
		
		
		
		// we are online, and the system was already initialized
    	if( mHostAdapter != null )
    		createOnlineLayout( );
    	
    	// initialize the ui for the first time
    	else if( mHostAdapter == null )
        {	
    		
    		MainActivity.this.runOnUiThread( new Runnable(){			    			
				@Override
				public void run() 
				{
					
					
				    try
			    	{	    							        	
				    	createOnlineLayout( );						    							    	
			    	}
			    	catch( Exception e )
			    	{
			    		Log.e(TAG,"Wyjatek "+e);    		
			    	}									
				}
			});		
				
			
        }
		
	
		
	}
	public void startNetworkDiscovery( boolean silent ) {
		stopNetworkDiscovery( silent );
		
		try {
			mNetworkDiscovery = new Discovery( this );
		} catch (UnknownHostException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	mNetworkDiscovery.start();
	
		
		if( silent == false )
			Toast.makeText( this, "Network discovery started.", Toast.LENGTH_SHORT ).show();
	}
	public void stopNetworkDiscovery( boolean silent ) {
		if( mNetworkDiscovery != null )
		{
			if( mNetworkDiscovery.isRunning() )
			{
				mNetworkDiscovery.exit();
				try
				{
					mNetworkDiscovery.join();
				}
				catch( Exception e )
				{
					// swallow
				}
				
				if( silent == false )
					Toast.makeText( this, "Network discovery stopped.", Toast.LENGTH_SHORT ).show();
			}
			
			mNetworkDiscovery = null;
		}
	}
	public String intToIp(int i) {

	   return ( i & 0xFF)  + "." +
	              
	               ((i >> 8 ) & 0xFF) + "." +
	               ((i >> 16 ) & 0xFF) + "." +
	                  ((i >> 24 ) & 0xFF ) ;
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.activity_main, menu);
		return true;
	}
	@Override
	public void onBackPressed() {
		if( mLastBackPressTime < java.lang.System.currentTimeMillis() - 4000 ) 
		{
			mToast = Toast.makeText( this, "Press back again to close this app.", Toast.LENGTH_SHORT );
			mToast.show();
			mLastBackPressTime = java.lang.System.currentTimeMillis();
		} 
		else
		{
			if( mToast != null ) 
				mToast.cancel();
		

					MainActivity.this.finish();
			
			mLastBackPressTime = 0;
		}
	}


	@Override
	public void onDestroy() {
		super.onDestroy();
		stopNetworkDiscovery( true );
					
		if( mHostReceiver != null )
			mHostReceiver.unregister();
		
		
				
		// make sure no zombie process is running before destroying the activity
		System.clean(  );		
						
				
		
		// remove the application from the cache
		java.lang.System.exit( 0 );
	}
	@Override
	protected void onListItemClick( ListView l, View v, int position, long id ){
		super.onListItemClick( l, v, position, id);

		stopNetworkDiscovery( true );		
		System.setCurrentHost( position );
		
		Toast.makeText( MainActivity.this, "Selected " + System.getCurrentHost().getAlias(), Toast.LENGTH_SHORT ).show();	                	
        startActivity
        ( 
          new Intent
          ( 
            MainActivity.this, 
            PortScanActivity.class
          ) 
        );
        //overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_left);                
	}
	
}

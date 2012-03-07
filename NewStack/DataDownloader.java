package NewStack;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;

import newNetwork.Connection;
import StateManagement.ContentState;
import StateManagement.StateManager;

public class DataDownloader extends Thread{
	
	StateManager stateManager;
	boolean execute;
	String localId;
	LinkDetector ldetector;
	List<String> localIPs ;
	public DataDownloader(String Id,StateManager manager,LinkDetector detector)
	{
		localId = Id;
		stateManager = manager;
		ldetector = detector;
		execute = true;
		localIPs = new ArrayList<String>();
	}
	
	public void close()
	{
		execute = false;
	}
	
	public void run()
	{
		while(execute)
		{
			try
			{
				List<String> requestedData = stateManager.getTCPDownloadRequests();
				Iterator<String> it = requestedData.iterator();
				while(it.hasNext())
				{
					String data = it.next();
					it.remove();
					ContentState stateObject = stateManager.getStateObject(data, ContentState.Type.tcpDownload);
					List<String> caches = stateObject.getPreferredRoute();
					if(!caches.isEmpty())
					{
						int offset = stateObject.getOffset();
						Connection.Type type = Connection.Type.values()[stateObject.getPreferredInterface()];
						boolean sendMetaDataFlag = stateObject.getMetaDataFlag(); 
						
						if(stateObject.currentSegments != stateObject.getTotalSegments())
						{
							
							String cache = caches.get(0);
							String[] cacheInformation = cache.split(":");
							String Id = cacheInformation[0];
							int port = Integer.parseInt(cacheInformation[1]);
							Registry registry = LocateRegistry.getRegistry(Id);
							IRMIServer stub = (IRMIServer) registry.lookup(new String(Id+"controlserver") );    
							ldetector.addDestination(Id+":"+port);
							stub.request_data(localId,data,offset,type,sendMetaDataFlag,stateObject.getTotalSegments(),stateObject.currentSegments);

						}
						
					}	
				}
				
				/*try
				{
					Thread.sleep(500);
				}catch(InterruptedException e)
				{
					e.printStackTrace();
				}
				*/
			}catch(Exception e)
			{
				e.printStackTrace();
			}

		}
	}
	
	@SuppressWarnings("unused")
	private void detectLink(){
		try 
		{
			List<String> temp = new ArrayList<String>();
			
			Enumeration<NetworkInterface> nets = NetworkInterface.getNetworkInterfaces();
			for(NetworkInterface netInf : Collections.list(nets))
			{
				if(!netInf.isPointToPoint() && !netInf.isLoopback())
				{
					Enumeration<InetAddress> inetAdd = netInf.getInetAddresses();
					for(InetAddress inet : Collections.list(inetAdd))
					{
						if(!temp.contains(inet.getHostAddress()))
						{
							temp.add(inet.getHostAddress());
						}
					}
				}
			}
		} 
		catch (SocketException e) 
		{
			e.printStackTrace();
		}
		
	}

}

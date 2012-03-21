package NewStack;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ConnectException;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

import javax.swing.*;


import org.apache.commons.io.FileSystemUtils;
import AbstractAppConfig.AppConfig;
import NewStack.Packet;
import StateManagement.ContentState;
import StateManagement.StateManager;
import prototype.datastore.DataStore;
import newNetwork.USBConnection;
import newNetwork.Connection;
import newNetwork.TCPConnection;

public class LinkDetector extends Thread{

	private String connectionId;
	private static List<String> destinationConnectionIds;
	private List<String> localIPs;
	private Scheduler scheduler;
	private boolean execute;
	private static int dataConnectionId;
	private static boolean flag = true ;
	private DataStore store;
	private DataStore DTNStore;
	private List<Integer> connectionPorts;	
	Map<String, ContentState> mpUp ;
	/**
	 * Newly added parameters for DTN Transfer
	 * @letters: for each drive
	 * @drives: for treating each drive as file
	 * @isDrive: for checking whether drives attached or not
	 */
	String[] letters = new String[]{ "A", "B", "C", "D", "E", "F", "G", "H", "I", 
            "J", "K", "L", "M", "N", "O", "P", "Q", "R",
            "S", "T", "U", "V", "W", "X", "Y", "Z" };
	File[] drives = new File[letters.length];
	boolean[] isDrive = new boolean[letters.length];
	private static List<String> dtnDestinationIds;
	
	public LinkDetector(String Id,Scheduler sched,List<Integer> portList,DataStore dStore,DataStore usbStore)
	{
		connectionId = Id;
		scheduler = sched;
		localIPs = new ArrayList<String>();
		execute = true;
		dataConnectionId = 0;
		connectionPorts = portList;
		store = dStore;
		DTNStore = usbStore;
		destinationConnectionIds = new ArrayList<String>();
		dtnDestinationIds = new ArrayList<String>();
		mpUp = new HashMap<String, ContentState>();
		
		for (int i = 0; i < letters.length; ++i )
	    {
	        drives[i] = new File(letters[i]+":/");
	        isDrive[i] = drives[i].canRead();
	    }

	}
	
	public void addDestination(String destination){
		/*try 
		{

			Enumeration<NetworkInterface> nets = NetworkInterface.getNetworkInterfaces();
			for(NetworkInterface netInf : Collections.list(nets))
			{
				if(!netInf.isPointToPoint() && !netInf.isLoopback())
				{
					Enumeration<InetAddress> inetAdd = netInf.getInetAddresses();
					for(InetAddress inet : Collections.list(inetAdd))
					{
						if(!localIPs.contains(inet.getHostAddress()))
						{
							localIPs.add(inet.getHostAddress());
						}
					}
				}
			}
		} 
		catch (SocketException e) 
		{
			e.printStackTrace();
		}*/

		try 
		{

			Enumeration<NetworkInterface> nets = NetworkInterface.getNetworkInterfaces();
			for(NetworkInterface netInf : Collections.list(nets))
			{
				if(netInf.isPointToPoint())
				{
					Enumeration<InetAddress> inetAdd = netInf.getInetAddresses();
					for(InetAddress inet : Collections.list(inetAdd))
					{
						if(!localIPs.contains(inet.getHostAddress()))
						{
							localIPs.add(inet.getHostAddress());
						}
					}
				}
			}
		} 
		catch (SocketException e) 
		{
			e.printStackTrace();
		}

		String[] connectionInfo = destination.split(":");
		InetAddress local;
		try
		{
			InetAddress add = InetAddress.getByName(connectionInfo[0]);
			int port = Integer.parseInt(connectionInfo[1]);
			System.out.println("Inside NewStack.LinkDetector: level1");
			//System.out.println("Port number is :"+ port);
			if(!destinationConnectionIds.contains(destination))
			{
				//System.out.println("Adding the Destinations in Scheduler");
				//System.out.println("local ip"+ localIPs);
				//System.out.println("local ip size:"+ localIPs.size());
				//Initially value of i=0(amit)
				for(int i = 4;i < localIPs.size();i++)
				{
					local = InetAddress.getByName(localIPs.get(i));
					System.out.println("Inside NewStack.LinkDetector: local value is :"+ local);
					if(!connectionPorts.isEmpty())
					{
						dataConnectionId++;
						Connection con;
						try 
						{
							//System.out.println("Adding the Destinations in Scheduler");
							con = new TCPConnection(dataConnectionId,add,port,local,connectionPorts.get(0));
							try{															
								connectionPorts.remove(0);
							}catch(Exception ex){
								ex.printStackTrace();
								System.out.println("Inside NewStack.LinkDetector: Exception line 160");
							}
							
							System.out.println("Inside NewStack.LinkDetector: New Connection created thru method addDes in Link Detct elseif");
							Packet packet = new Packet(connectionId); // authentication packet 
							con.writePacket(packet);
							flag = false ;
							System.out.println("Inside NewStack.LinkDetector: NEW Connection Established in link detectorthru method addDes in Link Detct elseif");
							scheduler.addConnection(connectionInfo[0],con);
							destinationConnectionIds.add(destination);
						}
						catch (ConnectException e)
						{
							e.printStackTrace();
							System.out.println("Inside NewStack.LinkDetector: Exception in the LinkDetecter.java");
							System.out.println("Inside NewStack.LinkDetector: Serever is terminated or Network unreachable");
							mpUp = StateManager.getUpMap();
							Set<String> key = mpUp.keySet();
							Iterator<String> it = key.iterator();
							while(it.hasNext())
							{
								String contentId = it.next();
								ContentState contentState = mpUp.get(contentId);
								if(contentState.getPreferredRoute().contains(destination))
								{	
									contentState.currentSegments = 0 ;
									mpUp.put(contentId,contentState);
									destinationConnectionIds.remove(destination);
								}	

							}

						}
						catch (IOException e)
						{
							e.printStackTrace();
							System.out.println("Inside NewStack.LinkDetector: Link Failure occured");
							if(!flag)
							{
								mpUp = StateManager.getUpMap();
								Set<String> key = mpUp.keySet();
								Iterator<String> it = key.iterator();
								while(it.hasNext())
								{
									String contentId = it.next();
									ContentState contentState = mpUp.get(contentId);
									/**
									 * Here below the value 100 should be configuration derived
									 * Right now we have 5 Queues and each queue contains 20 packets
									 * So that total loss at max is of 100 packets in case of Link failure 
									 */
									if(contentState.currentSegments > 200)
										contentState.currentSegments -= 200 ;
									else
										contentState.currentSegments = 0 ;
									mpUp.put(contentId,contentState);
									//destinationConnectionIds.remove(destination);

								}
								flag = true ;
							}

						}
					}	
				}	
			}
		}
		catch(Exception e)
		{
			e.printStackTrace();
			System.out.println("Inside NewStack.LinkDetector: exception line 235");
		}
	}
	public boolean addDTNDestination(String destination){
		boolean checkUSB = true ;
		String[] connectionInfo = destination.split(":");
		if(!dtnDestinationIds.contains(destination)){
			String dtnDir = findUSB(0);
			if(dtnDir != null){
				USBConnection con = new USBConnection(dtnDir);
				scheduler.addConnection(connectionInfo[0], con);
				dtnDestinationIds.add(destination);
			}
			else
				checkUSB = false ;
		}
		return checkUSB ;
	}
	
	public boolean addDTNDestination(String destination, int seg){
		boolean checkUSB = true ;
		String[] connectionInfo = destination.split(":");
		if(!dtnDestinationIds.contains(destination)){
			String dtnDir = findUSB(seg);
			if(dtnDir != null){
				USBConnection con = new USBConnection(dtnDir);
				scheduler.addConnection(connectionInfo[0], con);
				dtnDestinationIds.add(destination);
			}
			else
				checkUSB = false ;
		}
		return checkUSB ;
	}
	
	public void removeDTNdestination(String destination)
	{
		String[] connectionInfo = destination.split(":");
		dtnDestinationIds.remove(destination);
		scheduler.removeDTNConnection(connectionInfo[0]);
		
		JFrame parent = new JFrame();

	    JOptionPane.showMessageDialog(parent, "Now Safely remove drive");
	    
		System.out.println("Inside NewStack.LinkDetector: Safely remove drive:LinkDetector:removeDestination()");
		//JOptionPane.showMessageDialog(null,"Safely remove drive");
	}
	
	public void removeDestination(String destination)
	{
		String[] connectionInfo = destination.split(":");
		destinationConnectionIds.remove(destination);
		scheduler.removeConnection(connectionInfo[0]);
		//System.out.println("Safely remove drive:LinkDetector:removeDestination()");
		//JOptionPane.showMessageDialog(null,"Safely remove drive");
	}
	public String findUSB(int seg){
		String dtnDir = null ;
		for (int i = 0; i < letters.length; ++i)
        {
            boolean pluggedIn = drives[i].canRead();
            if (pluggedIn != isDrive[i])
            {
                if(pluggedIn)
                {	                
                    System.out.println("Inside NewStack.LinkDetector: Drive "+letters[i]+" has been plugged in");
                    String str = "cmd /c \"dir "+ letters[i] + ":\"";
                    boolean execute = true;
                    while(execute)
                    {
	                    try 
	                    {
							Process process =Runtime.getRuntime().exec(str);
							BufferedReader input = new BufferedReader(new InputStreamReader(process.getInputStream()));
							boolean file = false;
							
							if(findInUSB(input,file,"DTNRouter"))
							{
								System.out.println("Inside NewStack.LinkDetector: The USB key is a DTNRouter.\nPlease do not unplugged it until all request copied in the USB");
								dtnDir = letters[i] + ":DTNRouter\\";
								long spaceAvail = FileSystemUtils.freeSpaceKb(dtnDir);
								int dtnSize = Integer.parseInt(AppConfig.getProperty("NetworkStack.dtnSegmentSize"));
								System.out.println("Inside LinkDetector: DTN Size is "+ dtnSize);
								int spaceReq = 2*(dtnSize/1024)* seg ;
								System.out.println("Inside NewStack.LinkDetector: Available space is: "+spaceAvail+" space needed is: "+spaceReq);
								if(spaceAvail < spaceReq){
									return null ;
									//a compression function can be implement here
								}
								System.out.println("Inside NewStack.LinkDetector line 323");//line 323
							}
							execute = false; // to terminate the loop
						} 
	                    catch (IOException e) 
	                    {
							System.out.println("Inside NewStack.LinkDetector: Error in reading or writing file");
							try {
								Thread.sleep(5000);
							} catch (InterruptedException e1) {
								e1.printStackTrace();
							}
						}
                    }
                     
                }
                else
                {
                    System.out.println("Inside NewStack.LinkDetector: Drive "+letters[i]+" has been unplugged");
                }

                isDrive[i] = pluggedIn;
            }
        }
		
		return dtnDir ;
	}
	public static List<String> getDestinationIds()
	{
		return destinationConnectionIds ;
		
	}
	
	public static void setDestinationIds(List<String> destinationIds)
	{
		destinationConnectionIds = destinationIds ;
		System.out.println("Inside NewStack.LinkDetector: destinationConnectionIds in Link Detector: " + destinationConnectionIds);
		
	}
	public void close()
	{
		execute = false;
	}

	public void run()
	{

		boolean DTNlink = false;

		while(execute)
		{

			//************************ ADD USB detection*******************************
			if(DTNStore != null)
			{
				File DTNStatusFile = DTNStore.getFile(AppConfig.getProperty("DTN.Router.StatusFile"));
				StateManager usbStateManager = new StateManager(null);
				if(DTNStatusFile.exists() && !DTNlink)
				{
					System.out.println("Inside LinkDetector: Found DTN link :)");		
					File configFile = store.getFile(AppConfig.getProperty("DTN.Router.ConfigFile"));
					Properties Config = new Properties();
					FileInputStream fis;
					try {
						fis = new FileInputStream(configFile);
						Config.load(fis);
						fis.close();
					} catch (FileNotFoundException e) {
						e.printStackTrace();
					}catch (IOException e) {
						e.printStackTrace();
					} 

					File DTNconfigFile = DTNStore.getFile(AppConfig.getProperty("DTN.Router.ConfigFile"));
					Properties DTNConfig = new Properties();
					FileInputStream DTNfis;
					try {
						DTNfis = new FileInputStream(DTNconfigFile);
						DTNConfig.load(DTNfis);
						DTNfis.close();
					} catch (FileNotFoundException e) {
						e.printStackTrace();
					}catch (IOException e) {
						e.printStackTrace();
					} 

					String connectionId = Config.getProperty("DTNId");
					String DTNconnectionId = DTNConfig.getProperty("DTNId");

					//create a stack for end system 
					NewStack stack = new NewStack(DTNconnectionId,usbStateManager,DTNStore,null);

					//create Queue to communicate
					//readQueue reads from USb and writeQueue writes to USB
					BlockingQueue<Packet> readQueue = new ArrayBlockingQueue<Packet>(40);			//config driven
					BlockingQueue<Packet> writeQueue = new ArrayBlockingQueue<Packet>(40);			//config driven
					//create a USb connection
					USBConnection usbCon = new USBConnection(readQueue,writeQueue);
					//create a USB Connection
					USBConnection usbRouterCon = new USBConnection(writeQueue,readQueue);
					scheduler.addConnection(DTNconnectionId, usbCon);
					stack.addConnection(connectionId, usbRouterCon);
					DTNlink = true;
				}
				else if(DTNlink && !DTNStatusFile.exists())
				{
					DTNlink = false;
				}
			}
			
			try{
				InetAddress host = InetAddress.getLocalHost();
				InetAddress[] IPs = InetAddress.getAllByName(host.getHostName());
				for(int j = 0 ; j < IPs.length ; j++){
					String ip = IPs[j].getHostAddress();
					if(!localIPs.contains(ip))
					{
						System.out.println("Inside NewStack.LinkDetector: IP:" + ip);
						InetAddress localIP = IPs[j];
						for(int i = 0;i < destinationConnectionIds.size();i++){
							dataConnectionId++;
							if(!connectionPorts.isEmpty())
							{
								String destination = destinationConnectionIds.get(i);
								String[] connectionInfo = destination.split(":");
								InetAddress IP = InetAddress.getByName(connectionInfo[0]);
								int port = Integer.parseInt(connectionInfo[1]);
								Connection con = new TCPConnection(dataConnectionId,IP,port,localIP,connectionPorts.get(0));
								connectionPorts.remove(0);
								System.out.println("Inside NewStack.LinkDetector: New Connection created in run method");
								Packet packet = new Packet(connectionId);
								con.writePacket(packet);
								System.out.println("Inside NewStack.LinkDetector: NEW Connection Established in link detector in run method");
								scheduler.addConnection(connectionInfo[0],con);
							}
							else
								System.out.println("Inside NewStack.LinkDetector: local port unavailable");
						}

						localIPs.add(ip);

					}
					
				}
			}
			catch (Exception ioe){ System.out.println(ioe);
			ioe.printStackTrace();
			}
		}

	}

	private boolean findInUSB(BufferedReader input,boolean file,String name) throws IOException
    {
    	String line = "";
    	do
		{
			line = input.readLine();			
			if(line == null)
			{
				break;
			}
			if(line.contains("DTNRouter"))
			{
				if(line.contains("<DIR>"))
				{					
					return true;
				}				
			}		
		}while(line!=null);						
    	
		return false;    	
    }

}
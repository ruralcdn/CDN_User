package NewStack;

import java.net.*;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.*;

import StateManagement.Status;

import prototype.custodian.ICustodian;

import AbstractAppConfig.AppConfig;

public class DynamicIP extends Thread {
	private static DynamicIP myIP ;
	private boolean flag = false;
	private boolean startCheck = false ;
	private String userId ;
	private String oldIPAddressGPRS = null;
	String controlIP = "";
	private  DynamicIP(){ 
		userId = AppConfig.getProperty("User.Id");
	}	
    public static synchronized DynamicIP getIP(){
    	if(myIP == null)
    		myIP = new DynamicIP();
    	return myIP;
    }
    
    @SuppressWarnings("deprecation")
	public void run(){
    	System.out.println("Inside NewStack.DynamicIP: Starting DynamicIP");
    	while(true){
    		try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
    		
    		flag = true ;
    		startCheck = true ;
    		String localIP = detectPPP();
    		if(!localIP.equals(controlIP)){
    			controlIP = localIP ;
    			String custodian = "";
    			String[] splitInfo = controlIP.split(",");
    			if(!splitInfo[0].equals("127.0.0.1")){
    				if(splitInfo[1].equals("y"))
    					custodian = AppConfig.getProperty("User.Custodian.IP");
    				else
    					custodian = AppConfig.getProperty("User.Custodian.IP");
    				
    				try {
						Registry registry = LocateRegistry.getRegistry(custodian);
						
						// Changes by arvind 
						if(oldIPAddressGPRS == null || (!oldIPAddressGPRS.equals(splitInfo[0])))
						{
							ICustodian stub = (ICustodian) registry.lookup(AppConfig.getProperty("User.Custodian.Service") );
							stub.infoIP(userId,splitInfo[0]);
							oldIPAddressGPRS = splitInfo[0];
						}
						
					} catch (Exception e) {
						e.printStackTrace();
						System.out.println("Inside NewStack.DynamicIP: Error in locating service for Custodian");
						controlIP = "";
					}
    			}
    		}
    		
    		Status st = Status.getStatus();
    		if(!st.executeQuery("select * from status")){
    			flag = false ;
    			System.out.println("Inside NewStack.DynamicIP: Nothing in Status file");
    			suspend();
    		}
    	}
    }
    public String startThread(){
    	String str = "nop";
    	if(startCheck==false)
    		str = "start";
       	else if(flag == false)
    		str = "resume";
		return str ;
    }
    
    public String detectPPP(){
    	try 
		{
			List<String> linkPPP = new ArrayList<String>();
			List<String> linkDSL = new ArrayList<String>();
			Enumeration<NetworkInterface> nets = NetworkInterface.getNetworkInterfaces();
			
			//System.out.println("NETS VALUES"+" "+nets);
			
			//System.out.println("INITIALY VALUES"+" "+linkPPP);
			
			for(NetworkInterface netInf : Collections.list(nets))
			{
				//System.out.println("netInf VALUES"+" "+netInf);
				
				if(!netInf.isLoopback()){
					if(netInf.isPointToPoint())
					{
						//System.out.println("in point to point");
						//System.out.println("netInf VALUES"+" "+netInf);
						Enumeration<InetAddress> inetAdd = netInf.getInetAddresses();
						//System.out.println("inetAdd VALUES"+" "+inetAdd);
						for(InetAddress inet : Collections.list(inetAdd))
						{
							//System.out.println("inet vales in the ineer loop"+" "+inet);
							//System.out.println("linkPPP vales before add in the ineer loop"+" "+linkPPP);
							linkPPP.add(inet.getHostAddress());		//					
							//System.out.println("linkPPP vales after add in the ineer loop"+" "+linkPPP);
							//System.out.println("IP:"+" "+inet.getHostAddress());
						}
					}
					
					else 
					{
						Enumeration<InetAddress> inetAdd = netInf.getInetAddresses();
						for(InetAddress inet : Collections.list(inetAdd))
						{
							linkDSL.add(inet.getHostAddress());
						}
					}
				}
			}
			//System.out.println("FINAL VALUE of linkPPP"+" "+linkPPP.get(0));
			//System.out.println("FINAL VALUES DSL"+" "+linkDSL);
			int i = linkPPP.size();
			if(i!=0){				
				//System.out.println("linkPPP size is"+" "+linkPPP.size());
				return linkPPP.get(i-1)+",y";
				//return linkPPP.get(0)+",y";
			}				
			else if(linkDSL.size() != 0)
				return linkDSL.get(i-1)+",n";
				//return linkDSL.get(0)+",n";
			else 
		 		return "127.0.0.1,n";
		}	
		catch (SocketException e) 
		{
			e.printStackTrace();
		}
		
    	return "127.0.0.1,n";
    }
}

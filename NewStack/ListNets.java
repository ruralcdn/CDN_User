package NewStack;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;

public class ListNets extends Thread{
	private Enumeration<NetworkInterface> nets ;
	private InetAddress ipPPP;
	
	public ListNets(){
		try {
			ipPPP=InetAddress.getLocalHost();
			nets = NetworkInterface.getNetworkInterfaces();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	public void run(){
		while(true){
			try {
				List<InetAddress> ip = getPPP();
				if(ip.size() != 0 && !ip.get(0).equals(ipPPP) ){
					ipPPP = ip.get(0) ;
					String pppAd = ip.get(0).getHostAddress();
					System.out.println("PPP ip is "+pppAd);
					Runtime rt = Runtime.getRuntime();
					rt.exec("route add 124.124.247.2 "+pppAd);
				}
				else if(ip.size()==0){
					Runtime rt = Runtime.getRuntime();
					rt.exec("route delete 124.124.247.2");
				}
				
				Thread.sleep(10000);
			} catch (Exception e) {
				try{
					Thread.sleep(10000);
				}catch(Exception e1){
				}
				
				e.printStackTrace();
			}
		}
	}
	public List<InetAddress> getPPP() throws Exception{
		nets = NetworkInterface.getNetworkInterfaces();
		List<InetAddress> ipAdd = new ArrayList<InetAddress>();
		for(NetworkInterface netint : Collections.list(nets)){
			try {
				if(netint.isPointToPoint()){
					Enumeration<InetAddress> ipAddress = netint.getInetAddresses();
					for(InetAddress inet : Collections.list(ipAddress))
						ipAdd.add(inet);
				}
			} catch (SocketException e) {
				e.printStackTrace();
			}
		}
		return ipAdd ;
	}
	
	
	public List<InetAddress> getLocalIps(){
		List<InetAddress> inetAddress = new ArrayList<InetAddress>()  ;
		for(NetworkInterface netint : Collections.list(nets)){
			try {
				if(!netint.isPointToPoint() && !netint.isLoopback() ){
					Enumeration<InetAddress> intAdd = netint.getInetAddresses();
					for(InetAddress inet : Collections.list(intAdd)){
						inetAddress.add(inet) ;
						
					}
				}
			} catch (SocketException e) {
				e.printStackTrace();
			}
			
		}
		return inetAddress ;
		
	}
	
	public static void main(String args[]){
		ListNets listNets = new ListNets();
		List<InetAddress> inetAdd = listNets.getLocalIps();;
		for(int i = 0 ; i < inetAdd.size(); i++){
			System.out.println("IP Address is "+inetAdd.get(i));
			
		}
		
		listNets.start();
	}

}

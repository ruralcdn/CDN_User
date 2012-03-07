package DBSync;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;

import NewStack.DynamicIP;
import NewStack.NewStack;
import StateManagement.ContentState;
import StateManagement.StateManager;
import StateManagement.Status;
import AbstractAppConfig.AppConfig;
import prototype.datastore.*;
import prototype.dbserver.IDBServer;

public class RSyncThumb extends Thread {
	DataStore store ;
	List<String> imgList ;
	String userId ;
	NewStack stack ;
	StateManager stateManager ;
	public RSyncThumb(String user, NewStack newstack, StateManager stateMgr){
		store = new DataStore(AppConfig.getProperty("User.DataLog.Directory.path"));
		imgList = new ArrayList<String>();
		userId = user ;
		stack = newstack ;
		stateManager = stateMgr;
	}
	@SuppressWarnings("deprecation")
	public void run(){
		Status st = Status.getStatus() ;
		while(true){
			if(imgList.size()==0){
				try{
					Thread.sleep(5000);
				}catch(Exception e){
					e.printStackTrace();
				}
				imgList = st.getThumbData();
				
			}	
			if(imgList.size()==0)
				continue ;
			String imgName = imgList.remove(0);
			if(store.contains(imgName) || st.executeQuery("select * from status where contentid = '"+imgName+"' and type = 0"))
				continue ;
			else{
				IDBServer stub = null ;
				DynamicIP dynamicIP = DynamicIP.getIP();
				String rsyncserver= AppConfig.getProperty("User.RSyncServer.IP"); 
				try {
					Registry registry = LocateRegistry.getRegistry(rsyncserver);
					stub = (IDBServer) registry.lookup(AppConfig.getProperty("User.RSyncServer.service") );
				} catch (Exception e) {
					e.printStackTrace();
					System.out.println("RMI Problem for RSyncServer in RSyncThumb");
				}
				/*String controlIP = dynamicIP.detectPPP();
				String[] splitInfo = controlIP.split(",");
				if(!splitInfo[0].equals("127.0.0.1")){
					if(splitInfo[1].equals("y"))
						rsyncserver = AppConfig.getProperty("User.RSyncServer.IP");
					else
						rsyncserver =AppConfig.getProperty("User.RSyncServer.IP");

					try {
						Registry registry = LocateRegistry.getRegistry(rsyncserver);
						stub = (IDBServer) registry.lookup(AppConfig.getProperty("User.RSyncServer.service") );
						stub.infoIP(userId, splitInfo[0]);
					} catch (Exception e) {
						e.printStackTrace();
						System.out.println("Error in locating service for RSyncServer");
					}
				}*/
				String toStartCheck = dynamicIP.startThread();
				System.out.println("In User, value of toStartCheck: "+toStartCheck);
				if(toStartCheck.equals("start"))
					dynamicIP.start();
				else if(toStartCheck.equals("resume"))
					dynamicIP.resume();
				int segments = 0;
				try {
					segments = stub.findImg(imgName, userId+":2081");
				} catch (RemoteException e) {
					e.printStackTrace();
				}
				BitSet bitMap = new BitSet(segments);
				String uploadSyncServer = AppConfig.getProperty("User.SyncServer.DataConnection.Server")+":"+ AppConfig.getProperty("User.SyncServer.DataConnection.Port");
				stack.addDestination(uploadSyncServer);
				List<String> destinations = new ArrayList<String>();
				destinations.add(uploadSyncServer);
				ContentState stateObject1 = new ContentState(imgName,0,bitMap,-1,destinations,segments/*size*/,0,ContentState.Type.tcpDownload,Integer.toString(1/*id*/),true);
				stateManager.setStateObject(stateObject1); 
			}	
		}
	}
}

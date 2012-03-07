package prototype.user;

import java.rmi.RemoteException;
import java.util.*;

import DBSync.RSyncClient;
import PubSubModule.Notification;
import StateManagement.ApplicationStateManager;
import StateManagement.StateManager;
import prototype.custodian.ICustodianSession;


public class AppFetcher  implements IAppFetcher {

	ICustodianSession session;
	ApplicationStateManager AppStateManager;
	StateManager stateManager;
	boolean execute;
	public static boolean upFlag ; 
	public Map<String,List<String>> contentKeyValueMap ;
	public AppFetcher(ApplicationStateManager appManager,StateManager manager, Map<String,List<String>> contentKeyValueMap2) throws RemoteException
	{
		execute = true;
		AppStateManager = appManager;
		stateManager = manager;
		upFlag = false ;
		contentKeyValueMap = contentKeyValueMap2 ;
	}

	public void uploadNotify(Notification notif)throws RemoteException
	{
		try
		{
			if(notif.getNotificationType() == Notification.Type.UploadAck)
			{
				String contentId = notif.getContent();
				String content = contentId.substring(0, contentId.lastIndexOf('.'));
				System.out.println("in AppFetcher contentId: "+content);
				
				AppStateManager.addUploadAcks(contentId);
				if(contentKeyValueMap.containsKey(content)){
					List<String> dbList = contentKeyValueMap.get(content);
					RSyncClient.statQueue.put(dbList);
					contentKeyValueMap.remove(content);
				}
				
			}
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
		
	}
	
	public void uploadLog() throws RemoteException{
		System.out.println("In AppFetcher's UploadLog");
		upFlag = true ;
	}
}
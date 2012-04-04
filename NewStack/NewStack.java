package NewStack;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.BlockingQueue;
import newNetwork.Connection;
import prototype.datastore.DataStore;
import AbstractAppConfig.AppConfig;
import StateManagement.StateManager;
import StateManagement.Status;

public class NewStack
{
	private String stackId;
	private StateManager stateManager;
	private DataStore store;
	private DataStore dbStore ;
	private LinkDetector detector;
	private int segmentSize = Integer.parseInt(AppConfig.getProperty("NetworkStack.SegmentSize"));
	private int dtnsegmentSize = Integer.parseInt(AppConfig.getProperty("NetworkStack.dtnSegmentSize"));
	private int logSegmentSize = Integer.parseInt(AppConfig.getProperty("NetworkStack.logSegmentSize"));
	private Scheduler scheduler;
	private PolicyModule policyModule;
	private TCPServer server;
	private SegmentationReassembly sar;
	private static DataUploader uploader;
	private DataDownloader downloader;
	Reassembler reassembler;
	private DTNReader dtnReader;

	public NewStack(String localId,StateManager manager,DataStore dStore,DataStore usbStore,BlockingQueue<String> downloads,int serverPort,List<Integer> connectionPorts)
	{
		stackId = localId;
		stateManager = manager;
		store = dStore;
		policyModule = new PolicyModule();
		scheduler = new Scheduler(policyModule,stateManager,segmentSize);
		sar = new SegmentationReassembly(stateManager,store,dbStore,scheduler,segmentSize,dtnsegmentSize,logSegmentSize,downloads);
		if(serverPort != -1)
		{
			server = new TCPServer(scheduler,serverPort);
			new Thread(server).start();
			
		}
		detector = new LinkDetector(stackId,scheduler,connectionPorts,dStore,usbStore);
		uploader = new DataUploader(sar.getSegmenter(),scheduler.getConnectionPool(),detector,stateManager,scheduler.getDataEmptyQueues(),policyModule);
		downloader = new DataDownloader(stackId,stateManager,detector);
		Status st = Status.getStatus();
		DynamicIP dynamicIP = DynamicIP.getIP();
		if(st.executeQuery("select * from status"))
			dynamicIP.start();
		int size = stateManager.getTCPUploadRequests().size();
		if(size >= 1){
			System.out.println("Inside NewStack.NewStack: Starting the uploader with size: "+size);
			uploader.start();
		}
		
	}
	
	public NewStack(String localId,StateManager manager,DataStore dStore,DataStore usbStore,DataStore dst,BlockingQueue<String> downloads,int serverPort,List<Integer> connectionPorts,List<String> readList)
	{
		/*
		 * localId is is the userId like user1/user2
		 * manager is the object of state manager
		 * dStore is the location of directory from where all files suppose to be 
		 * uploaded this entry can be modified by changing entry in file user.config 
		 * usbStore is the path of usbStore location
		 * dst is the location where we have to put dbsync files
		 * Serverport value 2080
		 * connectionport is the of port [2082 2083 . . . . . 2102] 
		*/
		System.out.println("Inside NewStack.NewStack: Method NewStack");
		stackId = localId;
		stateManager = manager;
		store = dStore;
		dbStore = dst ;
		policyModule = new PolicyModule();
		scheduler = new Scheduler(policyModule,stateManager,segmentSize);
		sar = new SegmentationReassembly(stateManager,store,dbStore,scheduler,segmentSize,dtnsegmentSize,logSegmentSize,downloads);
		if(serverPort != -1)
		{
			server = new TCPServer(scheduler,serverPort);
			new Thread(server).start();
			
		}
		detector = new LinkDetector(stackId,scheduler,connectionPorts,dStore,usbStore);
		uploader = new DataUploader(sar.getSegmenter(),scheduler.getConnectionPool(),detector,stateManager,scheduler.getDataEmptyQueues(),policyModule);
		downloader = new DataDownloader(stackId,stateManager,detector);
		//System.out.println("Exception in connection2");
		Status st = Status.getStatus();
		DynamicIP dynamicIP = DynamicIP.getIP();
		if(st.executeQuery("select * from status"))
			dynamicIP.start();
		int size = stateManager.getTCPUploadRequests().size();
		if(size >= 1){
			System.out.println("Inside NewStack.NewStack: Starting the uploader with size: "+size);
			uploader.start();
		}
		dtnReader = new DTNReader(readList,stateManager,store,dtnsegmentSize,downloads);
		dtnReader.start();
	}

	public NewStack(String localId,StateManager manager,DataStore dStore,BlockingQueue<String> downloads)
	{
		stackId = localId;
		stateManager = manager;
		store = dStore;
		policyModule = new PolicyModule();
		scheduler = new Scheduler(policyModule,stateManager,segmentSize);
		sar = new SegmentationReassembly(stateManager,store,dbStore,scheduler,segmentSize,dtnsegmentSize,logSegmentSize,downloads);
		uploader = new DataUploader(sar.getSegmenter(),scheduler.getConnectionPool(),null,stateManager,scheduler.getDataEmptyQueues(),policyModule);
	}

	public void addDestination(String destination)
	{
		detector.addDestination(destination);
	}
	
	public void removeDestination(String destination)
	{
		detector.removeDestination(destination);
	}

	public int countSegments(String contentname)
	{
		return (int)sar.countSegments(contentname);
	}
	public int countDtnSegments(String contentname)
	{
		return (int)sar.countDtnSegments(contentname);
	}

	public void close()
	{
		detector.close();
		scheduler.close();
		sar.close();
		downloader.close();
	}

	public void addConnection(String remoteId,Connection con)
	{
		scheduler.addConnection(remoteId, con);
	}
	
	public List<String> getRoute(String destination)
	{
		return RouteFinder.findDTNRoute(destination);
	}
	
	public void setPolicy(String Id,Connection.Type type)
	{
		policyModule.setPolicy(Id, type);
	}
	public String getStackId()
	{
		return stackId;
	}
	public int getServerPort()
	{
		return server.getServerPort(); 
	}
	public String getDTNId()
	{
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
		return Config.getProperty("DTNId");
	}
	
	public static DataUploader getDataUploader(){
		return uploader;
	}
}
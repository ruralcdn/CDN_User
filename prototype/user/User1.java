package prototype.user;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.InetAddress;
import java.rmi.*;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.Date ;
import newNetwork.Connection;
import NewStack.DynamicIP;
import NewStack.ListNets;
import AbstractAppConfig.AppConfig;
import DBSync.RSyncClient;
import DBSync.RSyncThumb;
import NewStack.NewStack;
import StateManagement.ApplicationStateManager;
import StateManagement.ContentState;
import StateManagement.StateManager;
import StateManagement.Status;
import prototype.custodian.ICustodian;
import prototype.custodian.ICustodianSession;
import prototype.datastore.DataStore;
import prototype.dbserver.IDBServer;
import prototype.utils.AuthenticationFailedException;
import prototype.utils.NotRegisteredException;
import prototype.cache.ICacheServer;

public class User1 extends UnicastRemoteObject implements IUser {

	private static final long serialVersionUID = 1L;
	private static int AppId;
	int remotePort;
	boolean flag = true ;
	DataStore store; 
	DataStore dbStore ;
	ICustodianSession session;
	Map<String,ICustodianSession> userSession;
	ICacheServer cacheSession ;  
	StateManager stateManager;
	ApplicationStateManager appStateManager;
	File userConfiguration;
	NewStack stack;
	String remoteHost;
	static BlockingQueue<String> downloadList;
	BitSet bitMap ;
	String custodianId;
	Date d ;
	String userId;
	static Map<String,List<String>> contentKeyValueMap ;
	RSyncThumb rthumb ;
	public static boolean autoSync ;
	List<String> readingList ;
	public User1(String userid,DataStore dst ,DataStore st, ApplicationStateManager appStateMgr, StateManager stateMgr, NewStack netStack,List<String> readList) throws FileNotFoundException, IOException ,RemoteException
	{
		store = st;
		dbStore = dst ;
		stateManager = stateMgr ;
		appStateManager = appStateMgr ;
		AppId = 1;
		stack = netStack ;
		userSession = new HashMap<String,ICustodianSession>();
		userId = userid ;
		readingList = readList;
		// Commented to test without DBSync
		//rthumb = new RSyncThumb(userId,stack,stateManager);
		//rthumb.start();
	}

	public String login(String username, String password) throws IOException, NotBoundException,RemoteException
	{
		
		System.out.println("Here In User's login() method");
		autoSync = false;
		System.out.println("Value of auto in User1's login: "+autoSync);
		Registry registry = null ;
		ICustodian stub = null;
		
		// Code to retrieve dynamic IP of GPRS interface
		String controlIP = "";
		DynamicIP dynamicIP = DynamicIP.getIP();
		String localIP = dynamicIP.detectPPP();
		if(!localIP.equals(controlIP))
		{
    			controlIP = localIP ;
    			String[] splitInfo = controlIP.split(",");
    			controlIP = splitInfo[0];
		}
		
		
		ListNets listNets = new ListNets();
		try {
			if(listNets.getPPP().size()!=0)
				custodianId = AppConfig.getProperty("User.Custodian.IP");
			else
				custodianId = AppConfig.getProperty("User.Custodian.IP");
		} catch (Exception e1) {
			e1.printStackTrace();
		}
		System.out.println("User in "+custodianId );
		registry = LocateRegistry.getRegistry(custodianId);
		boolean bound = false;
		while(!bound)
		{
			try
			{
				stub = (ICustodian) registry.lookup(AppConfig.getProperty("User.Custodian.Service") );
				System.out.println("Value of stub returned "+stub);
				bound = true;
			}
			catch(Exception ex)
			{
				try
				{
					Thread.sleep(1000);
					System.out.println("Stub is not found with execption: "+ex);
				} 
				catch (InterruptedException e) 
				{
					e.printStackTrace();
				}
			}
		}
		
		System.out.println("Custodian Service Found.");
		
		try
		{
			System.out.println("Calling authenticate in USER1.java In LogIn Method");
			String userId = AppConfig.getProperty("User.Id");
			ICustodianSession session = stub.authenticate(username, password,userId, controlIP);
			if(session == null){
				System.out.println("Invalid username/password, Try Again");
				return null ;
			}
			userSession.put(username,session);
		}
		catch(NotRegisteredException e){
			System.out.println("NotRegisteredException");
		}
		catch(RemoteException e){
			System.out.println("Remote Exception happened");
			return null ;
		}
		catch(AuthenticationFailedException e)
		{
			e.printStackTrace();
			return null ;
		}
		return userId ;
	}

	
	public boolean registration(Map<String, String> userInfo) throws RemoteException{
		boolean regSuccess = false ;
		System.out.println("Here In User's registration() method");
		Registry registry = null ;
		ICustodian stub = null;
		ListNets listNets = new ListNets();
		try {
			if(listNets.getPPP().size()!=0)
				custodianId = AppConfig.getProperty("User.Custodian.PublicIP");
			else
				custodianId = AppConfig.getProperty("User.Custodian.IP");
		} catch (Exception e1) {
			e1.printStackTrace();
		}
		System.out.println("User in "+custodianId );
		registry = LocateRegistry.getRegistry(custodianId);
		boolean bound = false;
		while(!bound)
		{
			try
			{
				
				stub = (ICustodian) registry.lookup(AppConfig.getProperty("User.Custodian.Service") );
				System.out.println("Value of stub returned "+stub);
				bound = true;
			}
			catch(Exception ex)
			{
				try
				{
					Thread.sleep(1000);
					System.out.println("Stub is not found with execption: "+ex);
				} 
				catch (InterruptedException e) 
				{
					e.printStackTrace();
				}
			}
		}
		
		System.out.println("Custodian Service Found.");
		
		try
		{
			System.out.println("Calling registration in USER1.java In LogIn Method");
			//String userId = AppConfig.getProperty("User.Id");
			regSuccess = stub.new_registration(userInfo);
	
		}
		catch(Exception e){
			System.out.println("Remote Exception happened");
			return false ;
		}
		return regSuccess;
	}
	
	public boolean updateLog(String contentName, List<String> strList) throws RemoteException{
		boolean ins = false ;
		try {
			System.out.println("Content Name in updateLog is: "+contentName);
			contentKeyValueMap.put(contentName, strList);
			ins = true ;
		} catch (Exception e) {
			e.printStackTrace();
		}
		return ins;
			
	}
	
	public boolean updateLog(List<String> str) throws RemoteException{
		boolean ins = false ;
		try {
			RSyncClient.statQueue.put(str);
			ins = true ;
		} catch (Exception e) {
			e.printStackTrace();
		}
		return ins;
			
	}
	
	@SuppressWarnings("deprecation")
	public boolean delete(String contentId,String userName) throws RemoteException{
		boolean flag = false ;
		Registry registry = null ;
		try{
			DynamicIP dynamicIP = DynamicIP.getIP();
			String custodian= "" ;
			String controlIP = dynamicIP.detectPPP();
			String[] splitInfo = controlIP.split(",");
			if(!splitInfo[0].equals("127.0.0.1")){
				if(splitInfo[1].equals("y"))
					custodian = AppConfig.getProperty("User.Custodian.IP");
				else
					custodian =AppConfig.getProperty("User.Custodian.IP");
				
				try {
					registry = LocateRegistry.getRegistry(custodian);
					ICustodian stub = (ICustodian) registry.lookup(AppConfig.getProperty("User.Custodian.Service") );
					stub.infoIP(userId,splitInfo[0]);
				} catch (Exception e) {
					System.out.println("Error in locating service for Custodian in User1's upload");
				}
			}
			String toStartCheck = dynamicIP.startThread();
			System.out.println("In User, value of toStartCheck: "+toStartCheck);
			if(toStartCheck.equals("start"))
				dynamicIP.start();
			else if(toStartCheck.equals("resume"))
				dynamicIP.resume();
			ICustodianSession session = userSession.get(userName);
			flag = session.delete(contentId);
		}catch(Exception e){
			e.printStackTrace();
		}
		return flag;
		
	}
	public List<String> getUploadList() throws RemoteException 
	{
		List<String> uploadAckList = new ArrayList<String>();
		uploadAckList = appStateManager.getUploadAcks(); 
		return uploadAckList;
	}
	
	public List<String> getDownloadList(int AppId) throws RemoteException
	{
		List<String> AppIdDownloads = new ArrayList<String>();
		try
		{
			for(int i = 0;i < downloadList.size();i++)
			{
				String fileName = downloadList.poll();
				ContentState stateObject = stateManager.getStateObject(fileName,ContentState.Type.tcpDownload);
				if(stateObject.getAppId().equals(Integer.toString(AppId)))
				{
					AppIdDownloads.add(fileName);
				}
			}
			return AppIdDownloads;
		}catch(Exception e)
		{
			e.printStackTrace();
			return AppIdDownloads;
		}
	}
	
	public List<Integer> getUploadAcks (String contentName, int size) throws RemoteException{
		List<Integer> missingPackets = new ArrayList<Integer>();
		Map<String, ContentState> mpContent = StateManager.getDownMap(); 
		ContentState contentState = mpContent.get(contentName);
		if(contentState != null){
			BitSet bitSet = contentState.bitMap;
			for(int i = 0 ; i < size ;i++){
				if(bitSet.get(i)==false){
					missingPackets.add(i);
				}
			}
		}
		
		if(missingPackets.size()!=0){
			String node = "";
			if(contentName.contains(".log")){
				node = AppConfig.getProperty("User.SyncServer.DataConnection.Server")+":"+AppConfig.getProperty("User.SyncServer.DataConnection.Port");
			}
			else
				node = AppConfig.getProperty("User.Custodian.DataConnection.Server")+":"+ AppConfig.getProperty("User.Custodian.DataConnection.Port");
			stack.removeDestination(node);
			System.out.println("Adding Destination");
			stack.addDestination(node);
		}	
		return missingPackets;
	}
	
	@SuppressWarnings("deprecation")
	public String upload(String data,Connection.Type type,int id,String serviceInstance, String user) throws RemoteException
	{
		System.out.println("Inside User1.java: upload");
		String contentName = null;
		String myContentName = user;
		String fileType ="";
		System.out.println("Store value:"+store);//to be commented
		if(store.contains(data))
		{
			DynamicIP dynamicIP = DynamicIP.getIP();
			
			System.out.println("DynamicIP"+dynamicIP);//to be commented
			
			String custodian= "" ;
			
			String controlIP = dynamicIP.detectPPP();
			
			System.out.println("controlIP"+controlIP);//to be commented
			
			String[] splitInfo = controlIP.split(",");
			
			System.out.println("splitInfo"+controlIP);//to be commented
			
			if(!splitInfo[0].equals("127.0.0.1")){
				if(splitInfo[1].equals("y"))
					custodian = AppConfig.getProperty("User.Custodian.IP");
				else
					custodian =AppConfig.getProperty("User.Custodian.IP");
				
				System.out.println("Custodian value:"+custodian);//to be commented
				try {
					Registry registry = LocateRegistry.getRegistry(custodian);
					ICustodian stub = (ICustodian) registry.lookup(AppConfig.getProperty("User.Custodian.Service") );
					stub.infoIP(userId,splitInfo[0]);
				} catch (Exception e) {
					System.err.println("Error in locating service for Custodian in User1's upload");
				}
			}
			String toStartCheck = dynamicIP.startThread();
			System.out.println("In User, value of toStartCheck(User1): "+toStartCheck);
			if(toStartCheck.equals("start"))
				dynamicIP.start();
			else if(toStartCheck.equals("resume"))
				dynamicIP.resume();
			int segments = 0 ;
			int tcpSegment = 0 ;
			if(type != Connection.Type.USB)
				segments = stack.countSegments(data);
			else{
				segments = stack.countDtnSegments(data);
				tcpSegment = stack.countSegments(data);
			}
			bitMap = new BitSet(segments);
			System.out.println("Segments to be uploaded: "+segments);//to be commented
			System.out.println("TcpSegment to be uploaded: "+tcpSegment);//to be commented
			ICustodianSession session = userSession.get(user);
			int count = 0 ;
			do
			{
				try {					
					fileType = data.substring(data.lastIndexOf('.')+1);
					if(type != Connection.Type.USB)
						try{
							contentName = session.upload( myContentName, segments,serviceInstance,user,fileType);
						}catch(Exception ex){
							System.err.println("Content Id not generated:User1");
						}
					else
					{
						System.out.println("Calling session:: DTNUpload()");
						try{
							contentName = session.DTNUpload( myContentName, segments,tcpSegment,serviceInstance,user,fileType);
						}catch(Exception e){
							System.err.println("Content id not generated:CustodianSession");
						}
						System.out.println("Content name returned");
					}
							
					count = contentName.indexOf('$');
					myContentName = myContentName+"$"+contentName.substring(count+1);
					System.out.println("myContentName"+myContentName);
					System.out.println("Request for upload sent");
				} catch (Exception e) {
					System.err.println("Session has terminated, Please Login Again");
					return contentName;
				}

			}while(contentName.equals("DataServerNotFound") ||  contentName.equals("ServiceInstanceNotFound") || contentName.equals("RendezvousNotFound"));

			List<String> route = new ArrayList<String>();
			String uploadCustodian = AppConfig.getProperty("User.Custodian.DataConnection.Server")+":"+ AppConfig.getProperty("User.Custodian.DataConnection.Port");
			route.add(uploadCustodian);
			if(type != Connection.Type.USB){
				ContentState stateObject = new ContentState(data,contentName,0,bitMap, 
						Connection.Type.DSL.ordinal(),route,segments,0,ContentState.Type.tcpUpload,Integer.toString(id),true);
				stateManager.setTCPUploadState(stateObject);
				appStateManager.setServiceUploadName(myContentName, contentName, fileType);
				System.out.println("ContentName = " + contentName);
			}
			else{
				ContentState stateObject = new ContentState(data,contentName,0,bitMap,
						Connection.Type.USB.ordinal(),route,segments,0,ContentState.Type.dtn,Integer.toString(id),true);
				stateManager.setDTNState(stateObject);
				appStateManager.setServiceUploadName(myContentName, contentName, fileType);
				System.out.println("ContentName = " + contentName);
			}
							
		} 
		return contentName;
	}
		
	@SuppressWarnings("deprecation")
	public boolean uploadImg(String imgName) throws RemoteException{
		System.out.println("In uploadImg");
		boolean flag = false ;
		IDBServer stub = null ;
		DynamicIP dynamicIP = DynamicIP.getIP();
		String rsyncserver= "" ;
		String controlIP = dynamicIP.detectPPP();
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
		}
		
		String toStartCheck = dynamicIP.startThread();
		System.out.println("In User, value of toStartCheck: "+toStartCheck);
		if(toStartCheck.equals("start"))
			dynamicIP.start();
		else if(toStartCheck.equals("resume"))
			dynamicIP.resume();
		int segments = stack.countSegments(imgName) ;
		System.out.println("Number of segments: "+segments);
		stub.uploadThumb(imgName, segments);
		List<String> route = new ArrayList<String>();
		String dest = AppConfig.getProperty("User.SyncServer.DataConnection.Server")+":"+ AppConfig.getProperty("User.SyncServer.DataConnection.Port");
		route.add(dest);
		BitSet bitMap = new BitSet(segments);
		ContentState stateObject = new ContentState(imgName,imgName,0,bitMap, 
				Connection.Type.DSL.ordinal(),route,segments,0,ContentState.Type.tcpUpload,Integer.toString(1),true);
		stateManager.setTCPUploadState(stateObject);
		
		return flag ;
	}
	
	public String upload(String data,Connection.Type type,int id) throws RemoteException
	{

		System.out.println("User1.java: upload call from user to custodian ");
		String contentName = null;
		if(store.contains(data))
		{
			int segments = stack.countSegments(data);
			System.out.println("Segments = "+segments);

			if(type != Connection.Type.USB)
			{
				try {
					System.out.println("upload call from user to custodian");
					session.upload( contentName, segments);

					List<String> destinations = new ArrayList<String>();
					destinations.add(new String(remoteHost+":"+remotePort));
					ContentState stateObject = new ContentState(data,contentName,0, bitMap,
							Connection.Type.DSL.ordinal(),destinations,segments,0,ContentState.Type.tcpUpload,Integer.toString(id),true);
					stateManager.setTCPUploadState(stateObject);
					String fileType = null;
					appStateManager.setServiceUploadName(contentName, contentName,fileType);
			
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
			else
			{
				String destination;
				destination = session.DTNUpload( data, segments);
				List<String> route = stack.getRoute(destination);
				ContentState stateObject = new ContentState(data,contentName,0,bitMap,Connection.Type.USB.ordinal(),
						route,segments,0,ContentState.Type.dtn,Integer.toString(id),true);
				stateManager.setDTNState(stateObject);
				String fileType = null;
				appStateManager.setServiceUploadName(contentName, contentName,fileType);
			}
		}
		return contentName;
	}
   
	public String processDynamicContent(int id,String contentId,Connection.Type uploadType,Connection.Type downloadType,String dest) throws RemoteException
	{
		String uploadContentName = null;
		String downloadContentName = null;
		if(store.contains(contentId))
		{
			int segments = stack.countSegments(contentId);
			List<String> reply;
			reply = session.processDynamicContent(uploadContentName,segments, downloadContentName, uploadType, downloadType, dest); 
			if(reply != null)
			{
				int size = Integer.parseInt(reply.get(0));
				String serviceInstanceInfo = reply.get(1);
				if(uploadType != Connection.Type.USB)
				{
					try {
						List<String> destinations = new ArrayList<String>();
						destinations.add(new String(remoteHost+":"+remotePort));
						ContentState stateObject = new ContentState(contentId,uploadContentName,0,bitMap,
								uploadType.ordinal(),destinations,segments,0,ContentState.Type.tcpUpload,Integer.toString(id),true);
						stateManager.setTCPUploadState(stateObject);
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
				else
				{
					List<String> route = stack.getRoute(serviceInstanceInfo);
					ContentState stateObject = new ContentState(contentId,uploadContentName,0,bitMap,Connection.Type.USB.ordinal(),
							route,segments,0,ContentState.Type.dtn,Integer.toString(id),true);
					stateManager.setDTNState(stateObject);
				}

				try
				{
						List<String> destinations = new ArrayList<String>();
						destinations.add(new String(remoteHost+":"+remotePort));
						ContentState downloadStateObject = new ContentState(downloadContentName,0,bitMap,
								downloadType.ordinal(),destinations,size,0,ContentState.Type.tcpDownload,Integer.toString(id),true);
						if(downloadType != Connection.Type.USB)
						{
						stateManager.setTCPDownloadState(downloadStateObject);
						}
					else
					{
						stateManager.setStateObject(downloadStateObject);
					}
				}catch(Exception e)
				{
					e.printStackTrace();
				}
			}
		}

		return downloadContentName;

	}

	@SuppressWarnings("deprecation")
	public void find(String data,Connection.Type type,int id,String user) throws RemoteException{
		Status st = Status.getStatus();
		String str = "select * from status where contentid ='"+data+"' and type = 0";
		if(store.contains(data) || st.execQuery(str,data, 0)){
			System.out.println("Someone has submitted the download request or the data is in store");
			DynamicIP dynamicIP = DynamicIP.getIP();
			String custodian= "" ;
			String controlIP = dynamicIP.detectPPP();
			String[] splitInfo = controlIP.split(",");
			if(!splitInfo[0].equals("127.0.0.1")){
				if(splitInfo[1].equals("y"))
					custodian = AppConfig.getProperty("User.Custodian.IP");
				else
					custodian = AppConfig.getProperty("User.Custodian.IP");
				
				try {
					Registry registry = LocateRegistry.getRegistry(custodian);
					ICustodian stub = (ICustodian) registry.lookup(AppConfig.getProperty("User.Custodian.Service") );
					stub.infoIP(user,splitInfo[0]);
				} catch (Exception e) {
					System.out.println("Error in locating service for Custodian");
				}
			}
			String toStartCheck = dynamicIP.startThread();
			System.out.println("In User, value of toStartCheck: "+toStartCheck);
			if(toStartCheck.equals("start"))
				dynamicIP.start();
			else if(toStartCheck.equals("resume"))
				dynamicIP.resume();
			ICustodianSession session = userSession.get(user);
			System.out.println("Cache Found.");	
			String port = AppConfig.getProperty("User.port");
			session.find(id,data,type,userId+":"+port,0);
		}
		else{
			System.out.println("Your request has been submitted");
			try{
				DynamicIP dynamicIP = DynamicIP.getIP();
				String custodian= "" ;
				String controlIP = dynamicIP.detectPPP();
				String[] splitInfo = controlIP.split(",");
				if(!splitInfo[0].equals("127.0.0.1")){
					if(splitInfo[1].equals("y"))
						custodian = AppConfig.getProperty("User.Custodian.IP");
					else
						custodian = AppConfig.getProperty("User.Custodian.IP");
					
					try {
						Registry registry = LocateRegistry.getRegistry(custodian);
						ICustodian stub = (ICustodian) registry.lookup(AppConfig.getProperty("User.Custodian.Service") );
						stub.infoIP(user,splitInfo[0]);
					} catch (Exception e) {
						System.out.println("Error in locating service for Custodian");
					}
				}
				String toStartCheck = dynamicIP.startThread();
				System.out.println("In User, value of toStartCheck: "+toStartCheck);
				if(toStartCheck.equals("start"))
					dynamicIP.start();
				else if(toStartCheck.equals("resume"))
					dynamicIP.resume();
				ICustodianSession session = userSession.get(user);
				System.out.println("Cache Found.");	
				String port = AppConfig.getProperty("User.port");
				//int size = (int) session.find(id,data,type,userId+":"+port);
				int size = (int) session.find(id,data,type,userId+":"+port,1);
				System.out.println("Download call from user to cacheServer");
				if(type != Connection.Type.USB)
				{
					bitMap = new BitSet(size);
					String uploadCustodian = AppConfig.getProperty("User.Custodian.DataConnection.Server")+":"+ AppConfig.getProperty("User.Custodian.DataConnection.Port");
					stack.addDestination(uploadCustodian);
					List<String> destinations = new ArrayList<String>();
					destinations.add(uploadCustodian);
					ContentState stateObject = new ContentState(data,0,bitMap,-1,destinations,size,0,ContentState.Type.tcpDownload,Integer.toString(id),true);
					stateManager.setStateObject(stateObject);

				}
				else{
					bitMap = new BitSet(size);
					ContentState stateObject = new ContentState(data,0,bitMap,-1,null,size,0,ContentState.Type.tcpDownload,Integer.toString(id),true);
					stateManager.setStateObject(stateObject);
					readingList.add(data);
				}
					
			}catch(Exception e){
				e.printStackTrace();
			}
		}
	}
	public int getAppId()throws RemoteException{
		return AppId ;
	}
	
	public void uploadStatus(String name) throws RemoteException{
		stateManager.uploadStat(name);
		
	}

	public void logout(String user) throws RemoteException
	{
		userSession.remove(user);
		autoSync = true ; 
		System.out.println("Value of autoSync in Logut: "+autoSync);
		//session.close_connection();  
		//stack.close();
		
	}

	public static void main(String[] args) throws Exception{

		File configFile = new File("config/User.cfg");
		FileInputStream fis;
		fis = new FileInputStream(configFile);
		new AppConfig();
		AppConfig.load(fis);
		fis.close();
		autoSync = true ;
		System.out.println("Value of auto in User1's main: "+autoSync);
		String username = AppConfig.getProperty("User.Id");
		DataStore store = new DataStore(AppConfig.getProperty("User.Directory.path"));
		DataStore dbStore = new DataStore(AppConfig.getProperty("User.DataLog.Directory.path"));
		StateManager stateMgr = new  StateManager("status");
		ApplicationStateManager appStateMgr =  new ApplicationStateManager(store.getFile("status.cfg"));
		BlockingQueue<String> downloadList = new ArrayBlockingQueue<String>(Integer.parseInt(AppConfig.getProperty("User.MaximumDownloads")));
		List<String> readingList = new ArrayList<String>();
		//NewStack netStack = new NewStack(username,stateMgr,store,usbStore,downloadList,2080,portList);
		DataStore usbStore = null;
		if(AppConfig.getProperty("Routing.allowDTN").equals("1"))
		{
			usbStore = new DataStore(AppConfig.getProperty("User.USBPath"));
		}
						
		int port = Integer.parseInt(AppConfig.getProperty("User.NetworkStack.Port"));
		List<Integer> portList = new ArrayList<Integer>(20);							
		for(int i = 0;i < 20;i++)
		{
			portList.add(port);
			port++;
		}
		NewStack netStack = new NewStack(username,stateMgr,store,usbStore,dbStore,downloadList,2080,portList,readingList);
		
		// Commented to test without dbSync
		//RSyncClient rsyncClient = new RSyncClient(stateMgr,appStateMgr,netStack);
		//rsyncClient.start();
		
		contentKeyValueMap = new HashMap<String,List<String>>();
		AppFetcher fetcher = new AppFetcher(appStateMgr,stateMgr,contentKeyValueMap);
		IAppFetcher appFetcherStub = (IAppFetcher)UnicastRemoteObject.exportObject(fetcher,0);
		Registry appFetcherRegistry = LocateRegistry.getRegistry();
			
		boolean found = false;
		while(!found)
		{
			try
			{
				appFetcherRegistry.bind("appfetcher",appFetcherStub);
				found = true;
			}
			//-Djava.rmi.server.codebase=e:\\cdncdn.jar 
			//-Djava.security.policy=permission
			
			catch(AlreadyBoundException ex)
			{
				appFetcherRegistry.unbind("appfetcher");
				appFetcherRegistry.bind("appfetcher",appFetcherStub);
				found = true;
			}
			catch(ConnectException ex)
			{
				String rmiPath = AppConfig.getProperty("User.Directory.rmiregistry");
				Runtime.getRuntime().exec(rmiPath);
			}
		}		
				
		/**************Start the RMI Service named "user daemon" *****************************/
		
		ListNets listNets = new ListNets();
		List<InetAddress> host = listNets.getPPP();
						
		if(host.size()!=0){
			InetAddress hostIP = InetAddress.getByName(AppConfig.getProperty("User.Custodian.PublicIP"));
			
			//COMMENTED BY AMIT DUBEY
			//System.out.println("IP:"+hostIP.getHostAddress());
			//InetAddress thisIp =InetAddress.getLocalHost();
			//System.out.println("IP:"+thisIp.getHostAddress());
			
			String hostName = hostIP.getHostAddress();
			//System.out.println("host"+host);
			int i = host.size();
			//System.out.println("hostname is " + host.get(0).getHostAddress());
			Runtime.getRuntime().exec("route add "+hostName+" "+host.get(i-1).getHostAddress());
			//Runtime.getRuntime().exec("route add "+hostName+" "+host.get(0).getHostAddress());
			System.out.println("Value of PPP is "+host.get(i-1).getHostAddress());
		}	
		
		User1 obj = new User1(username,dbStore,store,appStateMgr,stateMgr,netStack,readingList);
		//System.getProperties().setProperty("java.rmi.server.hostname",AppConfig.getProperty("User.Id"));
		boolean foundNaming = false;
		while(!foundNaming)
		{
			try
			{
				Naming.bind(AppConfig.getProperty("User.Service"), obj);
				foundNaming = true;
			}
			catch(AlreadyBoundException ex)
			{
				Naming.unbind(AppConfig.getProperty("User.Service"));
				Naming.bind(AppConfig.getProperty("User.Service"), obj);
				foundNaming = true;
			}
			catch(ConnectException ex)
			{
				foundNaming = false;
				String rmiPath = AppConfig.getProperty("User.Directory.rmiregistry");
				Runtime.getRuntime().exec(rmiPath);
			}
		}
		System.err.println("Class User1: Server ready");		
	}
}

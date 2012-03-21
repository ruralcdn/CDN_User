package prototype.dataserver;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.rmi.registry.Registry;


import java.rmi.registry.LocateRegistry;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

import newNetwork.Connection;

import AbstractAppConfig.AppConfig;
import NewStack.NewStack;
import StateManagement.ContentState;
import StateManagement.ServiceInstanceAppStateManager;
import StateManagement.StateManager;
import prototype.datastore.DataStore;
import prototype.dataserver.IDataServer;


public class DataServer implements IDataServer {

	DataStore store;
	NewStack networkStack;
	StateManager stateManager;
	String cacheId;
	BlockingQueue<String> fileDownloads;
	ServiceInstanceAppStateManager AppManager;

	public DataServer(String Id) 
	{

		int port = Integer.parseInt(AppConfig.getProperty("DataServer.Port"));
		String path = AppConfig.getProperty("DataServer.Directory.Path");         //should be config driven
		store = new DataStore(path);
		


		DataStore usbStore = null;
		if(AppConfig.getProperty("Routing.allowDTN").equals("1"))
		{
			usbStore = new DataStore(AppConfig.getProperty("DataServer.USBPath"));
		}

		cacheId = Id;///configDriven
		File statusFile = store.getFile(AppConfig.getProperty("DataServer.StatusFile"));
		stateManager = new StateManager("statusdata");
		AppManager = new ServiceInstanceAppStateManager(statusFile);

		
		int maxDownloads = Integer.parseInt(AppConfig.getProperty("DataServer.MaximumDownloads"));
		BlockingQueue<String> fileDownloads = new ArrayBlockingQueue<String>(maxDownloads);   //config driven
		FileRegisterer registerer = new FileRegisterer(fileDownloads,AppManager);
		registerer.start();
		networkStack = new NewStack(cacheId,stateManager,store,usbStore,fileDownloads,port,new ArrayList<Integer>());

	}

	public void upload(String data,int size,String requester) throws RemoteException
	{
		System.out.println("Inside prototype.dataserver.DataServer: upload one");
		char[] bits = new char[size];

		for(int i = 0; i < size;i++)
		{
			bits[i] = '0';
		}
		BitSet bitMap = new BitSet();
		ContentState stateObject = new ContentState(data,0,bitMap,-1,null,size,0,ContentState.Type.tcpDownload,cacheId,true);
		stateManager.setStateObject(stateObject);
		AppManager.setRequesterDetail(data, requester);
	}


	public int TCPRead(int AppId,String dataname,Connection.Type type,String conId) throws RemoteException
	{
		if(store.contains(dataname) && store.contains(dataname+".marker"))
		{
			return networkStack.countSegments(dataname);
		}
		else
			return -1;
	}

	public boolean DTNRead(int AppId,String dataname,String dataRequester,String conId) throws RemoteException
	{
	
		if(store.contains(dataname))
		{
		//	stack.setPolicy(1,Connection.Type.USB);
		//	stack.sendDTNSegments(AppId,dataname,dataRequester);
			return true;
		}
		else
			return false;		
	}

	
	public void notify(String data) throws RemoteException
	{

	}

	public static void Init(String Id) 
	{
		try {

			DataServer obj = new DataServer(Id);
			IDataServer stub = (IDataServer) UnicastRemoteObject.exportObject(obj, 0);

			// Bind the remote object's stub in the registry
			Registry registry = LocateRegistry.getRegistry();
			registry.bind(AppConfig.getProperty("DataServer.Service"), stub);
			System.err.println("Server ready");

		} catch (Exception e) {
			System.err.println("Server exception: " + e.toString());
			e.printStackTrace();
		}

	}

	public static void main(String args[]) { 

		String config = new String("config/DataServer.cfg");
		File configFile = new File(config);
		FileInputStream fis;
		System.out.println("Inside prototype.dataserver.DataServer: upload main");
		try {
			fis = new FileInputStream(configFile);
			new AppConfig();
			AppConfig.load(fis);			
			fis.close();
		}catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		String Id = AppConfig.getProperty("DataServer.Id");
		///Why do we need of init(): Quamar
		Init(Id);

	}
}

package DBSync;

import java.net.Socket;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.sql.*;
import java.util.*;
import java.util.concurrent.*;
import java.io.*;
import newNetwork.Connection;
import prototype.dbserver.IDBServer;
import prototype.user.AppFetcher;
import prototype.user.User1;
import AbstractAppConfig.AppConfig;
import NewStack.DynamicIP;
import NewStack.NewStack;
import NewStack.Reassembler;
import StateManagement.ApplicationStateManager;
import StateManagement.ContentState;
import StateManagement.StateManager;

public class RSyncClient extends Thread {
	BlockingQueue<Socket> clsocks ;
	Socket sock;
	java.sql.Connection con ;
	Statement stmt ;
	ResultSet rs ;
	List<String> dbStats ;
	public static BlockingQueue<List<String>> statQueue ;	
	StateManager stateManager ;
	NewStack stack ;
	String userId ;
	String path ;
	public RSyncClient(StateManager stateManager2, ApplicationStateManager appStateManager2, NewStack netStack){
		try{
 			Class.forName("com.mysql.jdbc.Driver");
 			con = DriverManager.getConnection("jdbc:mysql://localhost:3306/ruralcdn","root","abc123");
 			dbStats = new ArrayList<String>();
 			statQueue = new ArrayBlockingQueue<List<String>>(20);
 			stack = netStack;
 			stateManager = stateManager2;
 			userId = AppConfig.getProperty("User.Id");
 			path = AppConfig.getProperty("User.DataLog.Directory.path");
 			
 			
		}catch(Exception e)
		{
			e.printStackTrace();
		}
	}
		
	@SuppressWarnings("deprecation")
	public void run()
	{
		while(true){
			try {
				int autoSyncCount = 0 ;
				int syncinLogin = 0 ;
				
				// loop for 12*5000 millisecond sleep
				while(User1.autoSync == true ){
					Thread.sleep(5000);
					autoSyncCount++ ;
					if(autoSyncCount >= 12)
						break;
				}
				
				//Replacement for above code  - not sure
				//Thread.sleep(12*5000);
				
				List<String> dbQueries = statQueue.peek();
				while(dbQueries == null && User1.autoSync == false)
				{				 
					Random rand = new Random();
					int sleepValue = rand.nextInt(2000)+3000;
					Thread.sleep(sleepValue);
					dbQueries = statQueue.peek();
					syncinLogin++;
					if(syncinLogin >= 10)
						break ;
				}
				
				if(dbQueries != null)
				{
					stmt = con.createStatement();
					for(int i = 0; i < dbQueries.size();i++)
					{
						String query = dbQueries.get(i);
						try{stmt.execute(query);
						File outFile = new File(path+"cache_db.log"); // cache_db.log contains sql commands
						BufferedWriter writer = new BufferedWriter(new FileWriter(outFile,true));
						writer.write(query);
						writer.newLine();
						writer.close();
						}catch(Exception e){
							e.printStackTrace();
							//dbQueries.add(dbQueries.size(),query);
						}
					}
					statQueue.remove(dbQueries);
					stmt.close();
					
				}
				
				IDBServer stub = null ;
				String rsyncserver=  AppConfig.getProperty("User.RSyncServer.IP") ;
				try {
					Registry registry = LocateRegistry.getRegistry(rsyncserver);
					stub = (IDBServer) registry.lookup(AppConfig.getProperty("User.RSyncServer.service") );
				} catch (Exception e) {
					e.printStackTrace();
					System.out.println("Error in locating service for RSyncServer");
				}
				
				DynamicIP dynamicIP = DynamicIP.getIP();
				String toStartCheck = dynamicIP.startThread();
				System.out.println("In User, value of toStartCheck: "+toStartCheck);
				if(toStartCheck.equals("start"))
					dynamicIP.start();
				else if(toStartCheck.equals("resume"))
					dynamicIP.resume();
				
				
				logFileToSend(rsyncserver);
				int segments = stack.countSegments("upload.log") ;
				boolean idle = stub.upload(userId+".log", segments, userId);
				if(!idle){
					System.out.println("DBSync is locked");
					Random rand = new Random();
					int sleepValue = rand.nextInt(1000)+5000;
					Thread.sleep(sleepValue);
					continue ;
				}	
				
				List<String> route = new ArrayList<String>();
				String dest = AppConfig.getProperty("User.SyncServer.DataConnection.Server")+":"+ AppConfig.getProperty("User.SyncServer.DataConnection.Port");
				route.add(dest);
				BitSet bitMap = new BitSet(segments);
				ContentState stateObject = new ContentState("upload.log",userId+".log",0,bitMap, 
						Connection.Type.DSL.ordinal(),route,segments,0,ContentState.Type.tcpUpload,Integer.toString(1),true);
				stateManager.setTCPUploadState(stateObject);
				
				
				segments = stub.find("download.log", userId+":2081");
				bitMap = new BitSet(segments);
				String uploadSyncServer = AppConfig.getProperty("User.SyncServer.DataConnection.Server")+":"+ AppConfig.getProperty("User.SyncServer.DataConnection.Port");
				stack.addDestination(uploadSyncServer);
				List<String> destinations = new ArrayList<String>();
				destinations.add(uploadSyncServer);
				ContentState stateObject1 = new ContentState("download.log",0,bitMap,-1,destinations,segments/*size*/,0,ContentState.Type.tcpDownload,Integer.toString(1/*id*/),true);
				stateManager.setStateObject(stateObject1); 
				boolean flag = false ;
				//long startTime = System.currentTimeMillis();
				while(!flag){
					if(Reassembler.downLog && AppFetcher.upFlag){
						System.out.println("Upload and download happened");
						executeLogStatement();
						updateLogFile("download.log","cache_db.log", rsyncserver);
						
						stub.executeLog(userId);
						
						File dlog = new File(path+"download.log");
						File ulog = new File(path+"upload.log");
						if(dlog.exists())
							dlog.delete();
						if(ulog.exists())
							ulog.delete();
						flag = true ;
						Reassembler.downLog =AppFetcher.upFlag= false ;
						//statQueue.remove(dbQueries);
					}
					//long endtime  = System.currentTimeMillis();
					//if((endtime-startTime)> 100000)
					//	flag = true ;
					Thread.sleep(2000);
				}
				System.out.println("Value of autoSync is: "+User1.autoSync);	
			} 
			catch (Exception e) {
				e.printStackTrace();
			}
		}

	}
	
	public void logFileToSend(String client)throws Exception{
		File toBeRead = new File(path+"cache_db.log");
		File toBeSent = new File(path+"upload.log");
		Statement stmt = con.createStatement();
		stmt.execute("select updated_till from synctable where entity ='"+client+"'");
		ResultSet rs = stmt.getResultSet();
		int cp = 0 ;
		if(rs.next())
			cp = rs.getInt(1);
		rs.close();
		stmt.close();
		BufferedReader reader = new BufferedReader(new FileReader(toBeRead)); 
		BufferedWriter writer = new BufferedWriter(new FileWriter(toBeSent));
		String str;
		int count = 0 ;
		boolean blank = true ;
		while((str = reader.readLine())!=null){
			++count ;
			if(count<=cp){
				continue ;
			}	
			else{
				writer.write(str);
				writer.newLine();
				blank = false ;
			}
		}
		if(blank)
			writer.newLine();
		reader.close();
		writer.close();
		
	}
	
	
	public void executeLogStatement() throws Exception{
		File logFile = new File(path+"download.log");
		BufferedReader reader = new BufferedReader(new FileReader(logFile)); 
		String str ;
		Statement stmt = con.createStatement();
		while((str=reader.readLine())!=null && (str.startsWith("insert")||str.startsWith("delete")|| str.startsWith("update"))){
			//System.out.println("value of query is : "+str);
			stmt.execute(str);
		}
		reader.close();
	}
	
	
	public void updateLogFile(String downloadFile, String logFile, String Client){
		try{
			File temp = new File(path+downloadFile);
			File cache_log = new File(path+logFile);
			BufferedReader reader = new BufferedReader(new FileReader(temp)); 
			BufferedWriter writer = new BufferedWriter(new FileWriter(cache_log,true));
			String str;
			while((str = reader.readLine())!=null && str.length()!=0 &&(str.startsWith("insert")||str.startsWith("delete")|| str.startsWith("update"))){
				writer.write(str);
				writer.newLine();
			}
			reader.close();
			writer.close();
			BufferedReader reader1 = new BufferedReader(new FileReader(cache_log));
			int count = 0 ;
			while(reader1.readLine()!= null)
				++count ;
			reader1.close();
			Statement stmt = con.createStatement();
			stmt.execute("update synctable set updated_till ="+count+" where entity ='"+Client+"'");
			stmt.close();
			System.out.println("Value of cp after updating: "+count);
			
		}catch(Exception e)
		{
			e.printStackTrace();
		}
		
	}

}

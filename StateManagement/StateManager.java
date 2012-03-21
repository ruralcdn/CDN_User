package StateManagement;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.*;
import NewStack.DataUploader;
import NewStack.NewStack;
import prototype.utils.Utils;

public class StateManager
{

	private File status;
	private static final String tcpUploadRequest = new String("TCPUploadRequest");
	private static final String tcpDownloadRequest = new String("TCPDownloadRequest");
	private static final String dtnData = new String("DTNData");
	private String table;
	String[] att = new String[8];
	static Map<String,ContentState> contUpStateMap;
	static Map<String,ContentState> dtnUpStateMap; //Newly added
	static Map<String,ContentState> contDownStateMap;
	static Map<String,String> tcpRequestMap ;
	static Map<String,String> dtnRequestMap ;
	static List<String> uploadRequest ;
	static List<String> dtnRequest ;
	static DataUploader dataUp;
	
	public StateManager(String tableName)
	{
		table = tableName;
		Status st = Status.getStatus();
		contUpStateMap = new HashMap<String,ContentState>();
		dtnUpStateMap = new HashMap<String,ContentState>();
		contDownStateMap = st.getPendingStatus("select * from "+table+" where type ='0'");
		tcpRequestMap = new HashMap<String,String>();
		dtnRequestMap = new HashMap<String,String>();
		uploadRequest = new ArrayList<String>();
		dtnRequest = new ArrayList<String>();
		if(tcpRequestMap.size()==0){
			st = Status.getStatus();
			tcpRequestMap = st.getUploadRequsets("uploadrequest");
			Set<String> requestKey = tcpRequestMap.keySet(); 
			Iterator<String> it = requestKey.iterator();
			while(it.hasNext()){
				uploadRequest.add(it.next());
			}
		}	
		
		if(dtnRequestMap.size()==0){
			st = Status.getStatus();
			dtnRequestMap = st.getUploadRequsets("dtnrequest");
			Set<String> requestKey = dtnRequestMap.keySet(); 
			Iterator<String> it = requestKey.iterator();
			while(it.hasNext()){
				dtnRequest.add(it.next());
			}
		}	
		
	}

	public List<String> getTCPUploadRequests() 
	{
		return uploadRequest;
	}

	public  List<String> getTCPDownloadRequests() 
	{
		Properties state = new Properties();
		FileInputStream fis;
		try {
			fis = new FileInputStream(status);
			state.load(fis);
			fis.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}catch(IOException e){
			e.printStackTrace();
		}

		String downloadRequestsString = state.getProperty(tcpDownloadRequest);
		List<String> downloadRequests;
		if(downloadRequestsString != null)
			downloadRequests = Utils.parse(downloadRequestsString);
		else
			downloadRequests = new ArrayList<String>();

		return downloadRequests;
	}

	public  List<String> getDTNData()
	{
		return dtnRequest;
	}

	public  synchronized ContentState getStateObject(String contentId,ContentState.Type stateType)///sYNC
	{

		Status st = Status.getStatus();
		String uploadId ;
		ContentState stateObj;
		BitSet bitMap = null ;	
		int offset = -1 ;
		int preferredInterface = -1;
		int totalSegments = -1;
		int currentSegments = 0 ;
		String appId = null;
		List<String> preferredRoute = null;
		String str;
		int a = 0 ;
		int type ;
		if(stateType == ContentState.Type.tcpUpload)
			type = 1;  
		else if(stateType == ContentState.Type.dtn) //Newly added
			type = -1 ;
		else
			type = 0 ;
		boolean metaDataFlag = false;
				
		if(stateType == ContentState.Type.tcpUpload && tcpRequestMap.get(contentId)!= null)
			uploadId = tcpRequestMap.get(contentId);
		else if(stateType == ContentState.Type.dtn && dtnRequestMap.get(contentId)!= null)
			uploadId = dtnRequestMap.get(contentId);
		else
			uploadId = contentId ;
		try
		{
			if(!contUpStateMap.containsKey(uploadId) && !contDownStateMap.containsKey(uploadId) && !dtnUpStateMap.containsKey(uploadId))
			{
				att = st.execQuery("select * from "+ table+ " where contentid = '"+ uploadId + "' and type = '" + type + "'"); 				
				if(att!=null && att[0].length()!=0)
				{
					 offset = Integer.parseInt(att[0]);
					 preferredInterface = Integer.parseInt(att[1]);
					 totalSegments = Integer.parseInt(att[2]);
					 currentSegments = Integer.parseInt(att[7]);
					 appId = att[3];
					 a = Integer.parseInt(att[4]);
					 str = att[5]+":"+att[6];
					 preferredRoute = new ArrayList<String>();
					 preferredRoute.add(str);
					 bitMap = new BitSet(totalSegments);
					 bitMap.clear();
					 bitMap.set(0,currentSegments,true);			 
					 
				}				
			}			
		}catch(NumberFormatException e)
		{
			e.printStackTrace();
		}
		
		if(a==1)
			metaDataFlag = true;		

		if(stateType == ContentState.Type.tcpUpload)
		{
			String uploadName = uploadId;
			if(contUpStateMap.containsKey(uploadName))
			{				
				stateObj = contUpStateMap.get(uploadName);
				
			}
			else
			{
				stateObj = new ContentState(contentId,uploadName,offset,bitMap,preferredInterface,preferredRoute,totalSegments,currentSegments,stateType,appId,metaDataFlag);
				contUpStateMap.put(uploadName, stateObj);
			}			
		}
		else if(stateType == ContentState.Type.dtn)
		{
			String uploadName = uploadId;
			if(dtnUpStateMap.containsKey(uploadName))
			{				
				stateObj = dtnUpStateMap.get(uploadName);
				
			}
			else
			{
				stateObj = new ContentState(contentId,uploadName,offset,bitMap,preferredInterface,preferredRoute,totalSegments,currentSegments,stateType,appId,metaDataFlag);
				dtnUpStateMap.put(uploadName, stateObj);
			}		
			
		}
		else
		{
			if(offset == -1 && preferredInterface == -1 && preferredRoute == null && totalSegments == -1 && appId == null && !metaDataFlag && !contDownStateMap.containsKey(contentId))
				return null;
			else 
			{
				if(contDownStateMap.containsKey(contentId))
					stateObj = contDownStateMap.get(contentId);
				else
				    stateObj = new ContentState(contentId,offset,bitMap,preferredInterface,preferredRoute,totalSegments,currentSegments,stateType,appId,metaDataFlag);
			        contDownStateMap.put(contentId,stateObj);
			}    
		}

		return stateObj;
	}

	public  synchronized void setTCPDownloadState(ContentState stateObj)
	{
		Properties state = new Properties();
		FileInputStream fis;
		try {
			fis = new FileInputStream(status);
			state.load(fis);
			fis.close();

			String contentId = stateObj.getContentId();

			String downloadRequestsString = state.getProperty(tcpDownloadRequest);
			List<String> downloadRequests;
			if(downloadRequestsString != null)
				downloadRequests = Utils.parse(downloadRequestsString);
			else
				downloadRequests = new ArrayList<String>();

			if(!downloadRequests.contains(contentId))
			{
				downloadRequests.add(contentId);
				state.setProperty(tcpDownloadRequest,downloadRequests.toString());
			}

			FileOutputStream out = new FileOutputStream(status);
			state.store(out,"--FileUpload&Download Status--");
			out.close();
			setStateObject(stateObj);


		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}catch(IOException e)
		{
			e.printStackTrace();
		}
	}

	@SuppressWarnings("deprecation")
	public  synchronized void setTCPUploadState(ContentState stateObj) 
	{
		setStateObject(stateObj);
		Status st = Status.getStatus();
		//System.out.println(st);
		String contentId = stateObj.getContentId();
		String uploadId = stateObj.getUploadId();
		System.out.println(" Inside StateManager.java: ContentId and UploadId "+" "+contentId+" "+uploadId);
		String tname = "uploadrequest";
		tcpRequestMap = st.setUploadRequsets(tname,contentId,uploadId);
		//tcpRequestMap = st.setUploadRequsets("uploadrequest",contentId,uploadId);
		//System.out.println(tcpRequestMap);
		uploadRequest.add(contentId);
		System.out.println("Inside StateManager: List size is: "+tcpRequestMap.size());//list size zero check
		dataUp = NewStack.getDataUploader();
		if(tcpRequestMap.size()==1 && dtnRequestMap.size()==0)
		{
			if(dataUp.isRunning()){
				dataUp.start();
				System.out.println("Inside StateManager: DataUploader is running");
			}	
			else
			{
				dataUp.setExecute();
				dataUp.resume();
				System.out.println("Inside StateManager: DataUploader is resuming");
			}
					
		}
		
	}

	@SuppressWarnings("deprecation")
	public  void setDTNState(ContentState stateObj)
	{
		setStateObject(stateObj);
		Status st = Status.getStatus();
		String contentId = stateObj.getContentId();
		String uploadId = stateObj.getUploadId();
		dtnRequestMap = st.setUploadRequsets("dtnrequest",contentId,uploadId);
		dtnRequest.add(contentId);
		System.out.println("Inside StateManager: DTN list size is: "+dtnRequestMap.size());
		dataUp = NewStack.getDataUploader();
		if(tcpRequestMap.size() == 0 && dtnRequestMap.size()==1)
		{
			if(dataUp.isRunning()){
				dataUp.start();
				System.out.println("Inside StateManager:DataUploader is running");
			}	
			else
			{
				dataUp.setExecute();
				dataUp.resume();
				System.out.println("DataUploader is resuming");
			}
					
		}
		
	}

	
	public synchronized void setStateObject(ContentState stateObj)
	{
		Status st = Status.getStatus(); 
		int type = 1 ;
		String contentId = stateObj.getContentId(); 
		ContentState.Type stateType = stateObj.getStateType();
		if(stateType == ContentState.Type.tcpDownload)
			type = 0 ;
		else if(stateType == ContentState.Type.dtn) //Newly added
			type = -1 ;
		//if(type == 1)
		if(type == 1 || type == -1)
			contentId = stateObj.getUploadId();
		String[] con = null ;
		if(stateObj.getPreferredRoute() != null)
		{
			String s1 = stateObj.getPreferredRoute().toString();
			String s2 = s1.substring(1,s1.length()-1);
			con = s2.split(":");						
		}	
		else
		{
			con = new String[2];
			con[0] = "";
			con[1] = "";
		}
		
		int a = 0 ;
		if(stateObj.getMetaDataFlag())
			a=1;
		if(type==0)
			contDownStateMap.put(contentId,stateObj);
		else if(type == 1)
			contUpStateMap.put(contentId, stateObj);
		else //Newly added
			dtnUpStateMap.put(contentId, stateObj);
		st.insertData(table,contentId,type,stateObj.getTotalSegments(),stateObj.getCurrentSegments(),stateObj.getOffset(),stateObj.getPreferredInterface(),con[0],stateObj.getAppId(),a,con[1]);
			
	}

	public  void setTCPUPloadRequestList(List<String> uploadRequests)
	{
		Status st = Status.getStatus();
		Set<String> request = tcpRequestMap.keySet();
		Iterator<String> it = request.iterator();
		while(it.hasNext()){
			String temp = it.next();
			if(!uploadRequest.contains(temp))
				tcpRequestMap = st.setUploadRequsets("uploadrequest",temp);
		}
	}

	public  void setDTNRequestList(List<String> dtnRequests)
	{
		Status st = Status.getStatus();
		Set<String> request = dtnRequestMap.keySet();
		Iterator<String> it = request.iterator();
		while(it.hasNext()){
			String temp = it.next();
			if(!dtnRequest.contains(temp))
				dtnRequestMap = st.setUploadRequsets("dtnrequest",temp);
		}
	}

	public  void removeTCPUploadState(ContentState stateObj)
	{
		Properties state = new Properties();
		FileInputStream fis;
		try {
			fis = new FileInputStream(status);
			state.load(fis);
			fis.close();


			String uploadRequestsString = state.getProperty(tcpUploadRequest);
			List<String> uploadRequests;
			if(uploadRequestsString != null)
				uploadRequests = Utils.parse(uploadRequestsString);
			else
				uploadRequests = new ArrayList<String>();

			String contentId = stateObj.getContentId();

			if(uploadRequests.contains(contentId))
			{
				uploadRequests.remove(contentId);
				state.setProperty(tcpUploadRequest,uploadRequests.toString());
			}

			FileOutputStream out = new FileOutputStream(status);
			state.store(out,"--FileUpload&Download Status--");
			out.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}catch(IOException e)
		{
			e.printStackTrace();
		}

	}

	public  void removeTCPDownloadState(ContentState stateObj)
	{
		Properties state = new Properties();
		FileInputStream fis;
		try {
			fis = new FileInputStream(status);
			state.load(fis);
			fis.close();


			String downloadRequestsString = state.getProperty(tcpDownloadRequest);
			List<String> downloadRequests;
			if(downloadRequestsString != null)
				downloadRequests = Utils.parse(downloadRequestsString);
			else
				downloadRequests = new ArrayList<String>();

			String contentId = stateObj.getContentId();

			if(downloadRequests.contains(contentId))
			{
				downloadRequests.remove(contentId);
				state.setProperty(tcpDownloadRequest,downloadRequests.toString());
			}

			FileOutputStream out = new FileOutputStream(status);
			state.store(out,"--FileUpload&Download Status--");
			out.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}catch(IOException e)
		{
			e.printStackTrace();
		}

	}

	public  void removeDTNState(ContentState stateObj)
	{
		Properties state = new Properties();
		FileInputStream fis;
		try {
			fis = new FileInputStream(status);
			state.load(fis);
			fis.close();


			String dtnDataString = state.getProperty(dtnData);
			List<String> dtnDataList;
			if(dtnDataString != null)
				dtnDataList = Utils.parse(dtnDataString);
			else
				dtnDataList = new ArrayList<String>();

			String contentId = stateObj.getContentId();

			if(dtnDataList.contains(contentId))
			{
				dtnDataList.remove(contentId);
				state.setProperty(tcpDownloadRequest,dtnDataList.toString());
			}

			FileOutputStream out = new FileOutputStream(status);
			state.store(out,"--FileUpload&Download Status--");
			out.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}catch(IOException e)
		{
			e.printStackTrace();
		}
	}

	public  boolean containsTCPDownloadRequest(String dataname) {
		FileInputStream fis;
		Properties state = new Properties();
		try {
			fis = new FileInputStream(status);
			state.load(fis);
			fis.close();

			List<String> downloadRequest;
			String downloadRequestString = state.getProperty(tcpDownloadRequest);
			if(downloadRequestString != null)
				downloadRequest = Utils.parse(downloadRequestString);
			else
				downloadRequest = new ArrayList<String>();

			if(downloadRequest.contains(dataname))
				return true;
			else 
				return false;


		} catch (FileNotFoundException e) {
			e.printStackTrace();
			return false;
		}catch(IOException e)
		{
			e.printStackTrace();
			return false;
		}
	}
	public  void uploadStat(String name){
		FileInputStream fis ;
		Properties state = new Properties();
		try{
			fis = new FileInputStream(status);
			state.load(fis) ;
			fis.close();
			List<String> uploadRequest ;
			String uploadRequestString = state.getProperty(tcpUploadRequest);
			System.out.println("Inside StateManager: Uploaded files name: " + uploadRequestString) ;
			uploadRequest = Utils.parse(uploadRequestString);
			uploadRequest.remove(name);
			System.out.println("Inside StateManager: upload Request now is : "+ uploadRequest.toString());
			state.setProperty(tcpUploadRequest, uploadRequest.toString());
			FileOutputStream out = new FileOutputStream(status);
			state.store(out,"--FileUpload&Download Status--");
			
		}catch(Exception e){
			
		}
		
	}
	public static Map<String, ContentState> getUpMap()
	{
	   return contUpStateMap ;
	}
	
	public static Map<String, ContentState> getDTNupMap(){
		return dtnUpStateMap;
	}
	public static Map<String, ContentState> getDownMap()
	{
	   return contDownStateMap ;
	}
	
	public static Map<String, ContentState> getDtnMap()
	{
	   return contDownStateMap ;
	}
	    
}



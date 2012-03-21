package NewStack;

import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import prototype.custodian.ICustodian;
import prototype.dbserver.IDBServer;
import newNetwork.Connection;
import newNetwork.ControlHeader;
import StateManagement.ContentState;
import StateManagement.StateManager;

public class DataUploader extends Thread{

	private StateManager stateManager;
	private BlockingQueue<BlockingQueue<Packet>> emptyQueue;
	private Map<String,List<Connection>> connectionPool;
	private static boolean execute;
	private boolean pickTCPData;
	private Segmenter segmenter;
	BitSet bitMap ;
	LinkDetector ldetector;
	PolicyModule policyModule;
	Map<String, ContentState> mpContentState ;
	Map<String, ContentState> mpDownState ;
	Map<String,List<Integer>> pendingContent ;
	
	public DataUploader(Segmenter seg,Map<String,List<Connection>> cp,LinkDetector detector,StateManager manager,BlockingQueue<BlockingQueue<Packet>> eQ,PolicyModule policy)
	{
		segmenter = seg;
		connectionPool = cp;
		stateManager = manager;
		emptyQueue = eQ;
		execute = true ;
		ldetector = detector;
		policyModule = policy;
		pickTCPData = true;
		mpContentState = new HashMap<String, ContentState>();
		mpDownState = new HashMap<String, ContentState>();	
		pendingContent = new HashMap<String, List<Integer>>();
	}
	
	@SuppressWarnings("deprecation")
	public void run()
	{
		while(execute)
		{
			try 
			{
				BlockingQueue<Packet> packetQueue = emptyQueue.take(); 
				Set<String> destinationSet = connectionPool.keySet(); 
				if(pickTCPData)
				{
					pickTCPData = false;
					List<String> tcpUploadData = stateManager.getTCPUploadRequests();
					int size = tcpUploadData.size();
					int count = 0;
					if(size == 0) 
					{
						emptyQueue.put(packetQueue); 
						continue;
					}
					
					count++;  
					String request = tcpUploadData.remove(0);  
					ContentState stateObject = stateManager.getStateObject(request, ContentState.Type.tcpUpload);
					//ContentState stateObject = stateManager.getStateObject(request, ContentState.Type.dtn);
					System.out.println("Inside NewStack.Datauploader: state object:"+stateObject);
					String destination =" ";
					try{
						destination = stateObject.getPreferredRoute().get(0);
					}catch(NullPointerException ex){
						ex.printStackTrace();
					}
					System.out.println("NewStack.Inside Datauploader: level 1");
					String[] conInfo = destination.split(":");
					//System.out.println("Datauploader-conInfo:"+conInfo);
					// COMMENTED BY ARVIND FOR usb TESTING
					//ldetector.addDestination(destination);
					while(!destinationSet.contains(conInfo[0]) && count <= size)
					{
						tcpUploadData.add(tcpUploadData.size(),request);
						tcpUploadData = stateManager.getTCPUploadRequests();
						count++;
						request = tcpUploadData.remove(0);
						stateObject = stateManager.getStateObject(request, ContentState.Type.tcpUpload);
						try{
							destination = stateObject.getPreferredRoute().get(0);
						}catch(NullPointerException ex){
						ex.printStackTrace();
						}
						conInfo = destination.split(":"); 
						ldetector.addDestination(destination);
						System.out.println("Inside NewStack.Datauploader: Value of Count in DataUploader::run()= "+count) ;
						tcpUploadData.add(tcpUploadData.size(),request);//Now added
						
					}
					
					if(count > size)
						emptyQueue.put(packetQueue);
					else
					{	
						boolean flag = false ;
						ContentState downloadStateObject = stateManager.getStateObject(stateObject.getContentId(),ContentState.Type.tcpDownload);
						String bitMap;
						ControlHeader header = null;
						if(downloadStateObject == null)
						{
							flag = true ;
						}
						else{
							int downCurSeg = downloadStateObject.getCurrentSegments();
							if(downCurSeg == downloadStateObject.getTotalSegments())
								flag = true ;
						}
						bitMap = null ;
						if(flag)
						{
							if(stateObject.currentSegments != stateObject.getTotalSegments())
							{
								String finalDestination = stateObject.getPreferredRoute().get(0).split(":")[0];
								policyModule.setPolicy(finalDestination, Connection.Type.values()[stateObject.getPreferredInterface()]);
								header = new ControlHeader(stateObject.getAppId(),null,bitMap,stateObject.getOffset(),finalDestination,stateObject.getMetaDataFlag());
								segmenter.sendSegments(stateObject.getContentId(),stateObject.getUploadId(),header,packetQueue,stateObject.getTotalSegments(), stateObject.currentSegments);
								tcpUploadData.add(tcpUploadData.size(),request);
								
							}
							else
							{
								ICustodian stub = null ;
								IDBServer stub1 = null ;
								boolean custOrdb = true ;
								String host = "mycustodian";
								if(!conInfo[0].equals("mycustodian")){
									custOrdb = false ;
									host = "myrsyncserver";
								}	
								Registry registry = null;
								List<Integer> pendingPackets = new ArrayList<Integer>();
								String contentName = stateObject.getUploadId();
								boolean bound = false;
								while(!bound)
								{
									try
									{
									
										registry = LocateRegistry.getRegistry(host);
										if(custOrdb)
											stub = (ICustodian) registry.lookup("custodian");
										else
											stub1 = (IDBServer) registry.lookup("rsync");
										bound = true;						
									}
									catch(Exception ex)
									{
										ex.printStackTrace();
									}
								}
								System.out.println("Inside NewStack.DataUploader: Cache Found.");	
								try {
									
									if(pendingContent.containsKey(contentName)){
										
										pendingPackets = pendingContent.get(contentName);
										System.out.println("Inside NewStack.Datauploader: Number of pending packets is: "+pendingPackets.size());
										String finalDestination = stateObject.getPreferredRoute().get(0).split(":")[0];
										policyModule.setPolicy(finalDestination, Connection.Type.values()[stateObject.getPreferredInterface()]);
										ControlHeader header1 = new ControlHeader(stateObject.getAppId(),null,bitMap,stateObject.getOffset(),finalDestination,stateObject.getMetaDataFlag());
										segmenter.sendPendingPackets(stateObject.getContentId(),stateObject.getUploadId(),header1,packetQueue,pendingContent);
										tcpUploadData.add(tcpUploadData.size(),request);
										continue;
									}
									else{
										
										if(custOrdb){
											pendingPackets = stub.getUploadAcks(contentName,stateObject.getTotalSegments());
											Thread.sleep(10000);
										}
										else{
											pendingPackets = stub1.getUploadAcks(contentName,stateObject.getTotalSegments());
											Thread.sleep(1000);
										}	
										if(pendingPackets.size()!= 0){
											pendingContent.put(contentName, pendingPackets);
											System.out.println("Inside NewStack.Datauploader: Number of pending packets is: "+pendingPackets.size());
											String finalDestination = stateObject.getPreferredRoute().get(0).split(":")[0];
											policyModule.setPolicy(finalDestination, Connection.Type.values()[stateObject.getPreferredInterface()]);
											ControlHeader header1 = new ControlHeader(stateObject.getAppId(),null,bitMap,stateObject.getOffset(),finalDestination,stateObject.getMetaDataFlag());
											segmenter.sendPendingPackets(stateObject.getContentId(),stateObject.getUploadId(),header1,packetQueue,pendingContent);
											tcpUploadData.add(tcpUploadData.size(),request);
											continue;
										}
										else{
											System.out.println("Inside NewStack.Datauploader: There are no pending packets for the uploading content: "+contentName);
										}
									}
								} catch (RemoteException e) {
									System.out.println("Inside NewStack.Datauploader: Problem in contacting through RMI");
									tcpUploadData.add(tcpUploadData.size(),request);
									emptyQueue.put(packetQueue);
									continue;
								}
								List<String> dtnRequest = stateManager.getDTNData();
								if(tcpUploadData.size() == 0 && dtnRequest.size() == 0 )
								{
									mpContentState = StateManager.getUpMap();
									mpContentState.remove(request);
									execute = false ;
									stateManager.setTCPUPloadRequestList(tcpUploadData);
									System.out.println("Inside NewStack.Datauploader: Execute = " + execute);	
									emptyQueue.put(packetQueue);
									suspend();
																	
								}
								else{
									mpContentState = StateManager.getUpMap();
									mpContentState.remove(request);
									stateManager.setTCPUPloadRequestList(tcpUploadData);
									emptyQueue.put(packetQueue);
								}	
							}
						}
						else
							emptyQueue.put(packetQueue);
					}
				}	
				else
				{
					boolean intersect = false;
					String destination = null;
					String bitMap = null;
					pickTCPData = true;
					List<String> dtnData = stateManager.getDTNData();
					@SuppressWarnings("unused")
					int count = 0;
					int size = dtnData.size();
					if(size == 0)
					{
						pickTCPData = true;
						emptyQueue.put(packetQueue);
						continue;
					}
					String request = dtnData.remove(0);
					count++;
					ContentState stateObject = stateManager.getStateObject(request, ContentState.Type.dtn);
					List<String>  route = stateObject.getPreferredRoute();
					destination = route.get(0);
					//intersect = ldetector.addDTNDestination(destination);
					intersect = ldetector.addDTNDestination(destination, stateObject.getTotalSegments());
					if(!intersect ){
						dtnData.add(dtnData.size(),request);
						emptyQueue.put(packetQueue);
						System.out.println("Inside NewStack.Datauploader: No USB Key found");//amit dubey
						Thread.sleep(2000);
					}	
					else if(stateObject.currentSegments != stateObject.getTotalSegments())
					{
						route = stateObject.getPreferredRoute();
						String finalDestination = route.get(0).split(":")[0];
						ControlHeader header = new ControlHeader(stateObject.getAppId(),null,bitMap,stateObject.getOffset(),finalDestination,stateObject.getMetaDataFlag());
						policyModule.setPolicy(finalDestination, Connection.Type.USB);
						segmenter.sendDTNSegments(stateObject.getContentId(),stateObject.getUploadId() , header, packetQueue,stateObject.getTotalSegments(),stateObject.getCurrentSegments());
						dtnData.add(dtnData.size(),request);
						System.out.println("Inside NewStack.DataUploader:");
					}
					else{
						List<String> tcpRequest = stateManager.getTCPUploadRequests();
						if(dtnData.size()== 0 && tcpRequest.size()==0){
							stateManager.setDTNRequestList(dtnData);
							emptyQueue.put(packetQueue);
							System.out.println("Inside NewStack.Datauploader: No Upload request for either TCP or USB");
							ldetector.removeDTNdestination(destination);
							execute = false ;
							suspend();
							System.out.println("Inside NewStack.DataUploader: Block2");
						}
						else{
							System.out.println("Inside NewStack.Datauploader: Some Upload request for either TCP or USB");
			
							stateManager.setDTNRequestList(dtnData);
							emptyQueue.put(packetQueue);
							System.out.println("Inside NewStack.DataUploader: Block3");
						}
					}	
				}


			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}		

	}

	public boolean isRunning()
	{
		return execute;
	}
	
	public void setExecute()
	{
		execute = true ;
	}

}
package NewStack;

import java.io.File;
import java.util.BitSet;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.BlockingQueue;

import AbstractAppConfig.AppConfig;
import NewStack.Packet;
import StateManagement.ContentState;
import StateManagement.StateManager;
import StateManagement.Status;
import prototype.datastore.DataStore;

public class Reassembler extends Thread{

	private BlockingQueue<Packet> packetQueue;
	private DataStore store;
	private DataStore dbStore ;
	private boolean execute;
	public static boolean downLog ;
	private int segmentSize ;
	private int logSize ;
	private BlockingQueue<String> fileDownloads;
	StateManager stateManager;
	Map<String, ContentState> mpContent ;
	Status stat;
	
	public Reassembler(BlockingQueue<Packet> queue,DataStore st,DataStore dst,StateManager manager,int segmentsize,int logsize, BlockingQueue<String> downloads)
	{
		packetQueue = queue;
		store = st;
		dbStore = dst ;
		segmentSize = segmentsize ;
		logSize = logsize ;
		execute = true;
		stateManager = manager;
		fileDownloads = downloads;
		mpContent = new HashMap<String, ContentState>();
		stat = Status.getStatus();
		downLog = false ;
	}

	public void close()
	{
		execute = false;	
	}
	
	public void run()
	{
		while(execute)
		{
			try 
			{
				int size = segmentSize ;
				DataStore store = this.store ;
				Packet packet = packetQueue.take();
				mpContent = StateManager.getDownMap();
				if(mpContent == null)
					continue;

				if(packet.isMetaData())
				{
					System.out.println("Inside NewStack.Reassembler: Received a metaData Packet");
					String data = packet.getName();
					long offset = packet.getSequenceNumber();
					byte[] segment = packet.getData();
					store.write(data+SegmentationReassembly.metadataSuffix,offset,segment);
				}
				else
				{
				String data = packet.getName();
				if(data.contains(".log") || data.contains(".jpg")){
					size = logSize ;
					store = dbStore ;
				}	
				ContentState conProp ;
				BitSet bs ;
				try{
					conProp = mpContent.get(data);
					bs = conProp.bitMap;
				}catch(Exception e){
					continue ;
				}
				long offset = packet.getSequenceNumber();
				byte[] segment = packet.getData();
				store.write(data, offset, segment);

				if(stateManager != null)
				{
					try
					{
						int currentsegments = conProp.currentSegments; 						
						if(currentsegments == conProp.getTotalSegments())
						{
							
						}
						else
						{
							if(bs.get((int) (offset/size))==false)
							{
								currentsegments++;
								bs.set((int) (offset/size));
							}
							conProp.currentSegments = currentsegments;
							conProp.bitMap = bs ;
							mpContent.put(data,conProp);
							if(currentsegments == conProp.getTotalSegments())
							{
								System.out.println("In NewStack.Reassembler.java, received file "+data+"! :D :D ");
								if(data.contains(".log")){
									downLog = true ;
									System.out.println("Inside NewStack.Reassembler: Download Log received");
								}	
								if(data.contains(".jpg")){
									System.out.println("Inside NewStack.Reassembler: ThumbNail received ");
									String path = AppConfig.getProperty("User.DataLog.Directory.path");
									File imgName = new File(path+data);
									String Ipath = AppConfig.getProperty("imagepath");
									System.err.println("Inside NewStack.Reassembler: ImagePath"+ Ipath);
									imgName.renameTo(new File("C:\\Documents and Settings\\CDN\\workspace\\PSDITDEMO\\WebContent\\DBServer\\"+data));//amit
									//imgName.renameTo(new File("C:\\Users\\Administrator\\workspace\\PubSubApi\\WebContent\\DBServer\\"+data));
								}
								if(fileDownloads != null)
									fileDownloads.put(data);
								stat.updateState("status",data, 0);  
							}
						}
					}	
					catch(Exception e)    
					{
						e.printStackTrace();
					}
				}
			}
			} catch (Exception e) 
			{
				e.printStackTrace();
			}

		}
	}
}
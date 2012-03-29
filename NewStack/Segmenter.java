package NewStack;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import StateManagement.*;
import NewStack.Packet.PacketType;
import newNetwork.ControlHeader;
import prototype.datastore.DataStore;

public class Segmenter extends Thread{

	private DataStore store;
	private DataStore dbStore ;
	private int segmentSize;
	private int dtnSize ;
	private int logSize ;
	BlockingQueue<BlockingQueue<Packet>> fullQueue;
	Status status ;
	Map<String, ContentState> mpUp ;
	Map<String, ContentState> dtnUP;
	BitSet bits ;
	long start_time ;
	long end_time ;
	public Segmenter(DataStore st,DataStore dst, int size,int dtnsize,int logsize, BlockingQueue<BlockingQueue<Packet>> fQ)
	{
		store = st;
		dbStore = dst ;
		segmentSize = size;
		dtnSize = dtnsize;
		logSize = logsize ;
		fullQueue = fQ;
		status = Status.getStatus();
		mpUp = new HashMap<String, ContentState>();
		dtnUP = new HashMap<String, ContentState>();
		bits = new BitSet();
	}

	public void sendSegments(String readName,String sendName,ControlHeader ctrlHeader,BlockingQueue<Packet> packetQueue,int totSeg, int curSeg)
	{
		int size = segmentSize ;
		DataStore store = this.store;
		if(readName.contains(".log") || readName.contains(".jpg") || readName.contains(".jpeg")){
			size = logSize ;
			store = dbStore ;
		}	
		BlockingQueue<Packet> segmentQueue = packetQueue;
		List<String> route = null;
		String destination = null;
		mpUp = StateManager.getUpMap();
		
		if(ctrlHeader != null)
		{
			route = ctrlHeader.getRoute();
			destination = ctrlHeader.getDestination();
			ctrlHeader.getMetaDataFlag();
		}

		ContentState stateObj = mpUp.get(sendName);
		int j = stateObj.currentSegments;
		for( ;j < (totSeg)&& segmentQueue.remainingCapacity() > 0;j++)
		{
			byte[] segment = store.read(readName, j*size, size);
			try{Packet packet = new Packet(route,destination,PacketType.Data,sendName,segment,j*size,false);
			if(segmentQueue != null)
					segmentQueue.offer(packet);
			}catch(Exception e){
				System.out.println("Excepton in Segmenter");
			}
		}
		stateObj.currentSegments = j;		
		mpUp.put(sendName, stateObj);
		if(stateObj.currentSegments==stateObj.getTotalSegments())
		{
			Status st = Status.getStatus();
			st.updateState("status",sendName,1);
		}
		fullQueue.add(segmentQueue);
	}
	
	public void sendDTNSegments(String readName,String sendName,ControlHeader ctrlHeader,BlockingQueue<Packet> packetQueue,int totSeg, int curSeg)
	{
		
		BlockingQueue<Packet> segmentQueue = packetQueue;
		List<String> route = null;
		String destination = null;
		dtnUP = StateManager.getDTNupMap();
		
		if(ctrlHeader != null)
		{
			route = ctrlHeader.getRoute();
			destination = ctrlHeader.getDestination();
		}

		ContentState stateObj = dtnUP.get(sendName);
		if(stateObj.currentSegments == 0)
			start_time = System.currentTimeMillis();
		int j = stateObj.currentSegments;
		System.out.println("Inside NewStack.Segmenter: copying");
		for( ;j < (totSeg)&& segmentQueue.remainingCapacity() > 0;j++)
		{
			System.out.print(".");
			byte[] segment = store.read(readName, j*dtnSize, dtnSize);
			Packet packet = new Packet(route,destination,PacketType.Data,sendName,
					segment,j*dtnSize,false);
			if(segmentQueue != null)
					segmentQueue.offer(packet);
		}
		stateObj.currentSegments = j;		
		dtnUP.put(sendName, stateObj);
		if(stateObj.currentSegments==stateObj.getTotalSegments())
		{
			end_time = System.currentTimeMillis();
			System.out.println("Inside NewStack.Segmenter: copying end");
			System.out.println("Inside NewStack.Segmenter: Time spend in writing the file: "+(end_time-start_time));
			Status st = Status.getStatus();
			st.updateState("status",sendName,-1);
		}
		fullQueue.add(segmentQueue);
	}
	
	public void sendPendingPackets(String readName,String sendName,ControlHeader ctrlHeader,BlockingQueue<Packet> packetQueue,Map<String,List<Integer>> pendingContent){
		int size = segmentSize ;
		DataStore store = this.store;
		if(readName.contains(".log") || readName.contains(".jpg") || readName.contains(".jpeg")){
			size = logSize ;
			store = dbStore ;
		}
		BlockingQueue<Packet> segmentQueue = packetQueue;
		List<String> route = null;
		String destination = null;
		if(ctrlHeader != null)
		{
			route = ctrlHeader.getRoute();
			destination = ctrlHeader.getDestination();
		}

		List<Integer> pendingList = new ArrayList<Integer>();
		pendingList = pendingContent.get(sendName) ;
		int index = pendingList.size();
		for(int j = 0 ; j < index && segmentQueue.remainingCapacity() > 0;j++)
		{
			int segNo =pendingList.remove(0);
			byte[] segment = store.read(readName, segNo*size, size);
			Packet packet = new Packet(route,destination,PacketType.Data,sendName,
					segment,segNo*size,false);
			if(segmentQueue != null)
					segmentQueue.offer(packet);
			
		}
		if(pendingList.size()!=0)
			pendingContent.put(sendName,pendingList);
		else
			pendingContent.remove(sendName);
		fullQueue.add(segmentQueue);
	}
}
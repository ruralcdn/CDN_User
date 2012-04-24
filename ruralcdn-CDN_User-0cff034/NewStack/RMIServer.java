package NewStack;

import java.rmi.RemoteException;
import java.util.concurrent.BlockingQueue;

import newNetwork.Connection;
import newNetwork.ControlHeader;

import prototype.datastore.DataStore;


public class RMIServer implements IRMIServer{

	private DataStore store;
	private SegmentationReassembly sar;
	private PolicyModule policyModule;
	private BlockingQueue<BlockingQueue<Packet>> emptyQueue;

	public RMIServer(DataStore dStore,SegmentationReassembly sr,PolicyModule policy,BlockingQueue<BlockingQueue<Packet>> eQ)
	{
		store = dStore;
		sar = sr;
		policyModule = policy;
		emptyQueue = eQ;
	}

	public int request_data(String requesterId,String data,int offset,Connection.Type type,boolean sendMetaData,int totSeg, int curSeg) throws RemoteException
	{
		System.out.println("Inside NewStack.RMIServer: Level 1");
		
		if(store.contains(data) && store.contains(data+".marker") && type != Connection.Type.USB)
		{
			BlockingQueue<Packet> packetQueue = emptyQueue.poll();
			if(packetQueue != null)
			{
				System.out.println("Inside NewStack.RMIServer: Level 2");
				String bitMap = null ;
				Segmenter segmenter = sar.getSegmenter();
				ControlHeader header = new ControlHeader(requesterId,null,bitMap,offset,requesterId,sendMetaData);
				segmenter.sendSegments(data, data, header, packetQueue,totSeg,curSeg);
				policyModule.setPolicy(requesterId, type);
				return (int) sar.countSegments(data);
			}
			else
				return -1;
		}
		else
			return -1;
	}
}
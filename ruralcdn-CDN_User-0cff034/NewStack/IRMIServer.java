package NewStack;

import java.rmi.Remote;
import java.rmi.RemoteException;

import newNetwork.Connection;

public interface IRMIServer extends Remote{
	
	public int request_data(String requesterId,String data,int offset,Connection.Type type,boolean sendMetaData,int totSeg,int curSeg) throws RemoteException;
	
}
package prototype.cache;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.List;

import newNetwork.Connection;

public interface ICacheServer extends Remote{
	
	public void notify(String data) throws RemoteException; 
	//public String upload(String myContentName,int segments,String serviceInstance, String username) throws RemoteException ;
	public String upload(String myContentName,int segments,String serviceInstance, String username, String fileType) throws RemoteException ;
	public String dtnUpload(String myContentName,int segments,String serviceInstance, String username, String fileType) throws RemoteException ;
	public long find(int id,String data,Connection.Type type,String user) throws RemoteException;
	public List<Integer> getUploadAcks(String contentName, int size) throws RemoteException ;
}
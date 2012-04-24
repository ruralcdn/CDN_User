package prototype.dbserver;

import java.rmi.*;
import java.util.*;

public interface IDBServer extends Remote{
	public boolean upload(String contentName, int size, String dest) throws RemoteException ;
	public int find(String data,String dest) throws RemoteException ;
	public List<Integer> getUploadAcks(String ContentName, int totSeg) throws RemoteException;
	public void executeLog(String dest) throws RemoteException;
	public void infoIP(String userId, String IPadd) throws RemoteException ;
	public boolean uploadThumb(String contentId, int size) throws RemoteException ;
	public int findImg(String imgName, String userdaemonId) throws RemoteException;
}

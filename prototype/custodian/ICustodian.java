package prototype.custodian;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.List;
import java.util.Map;

import prototype.utils.AlreadyRegisteredException;
import prototype.utils.AuthenticationFailedException;
import prototype.utils.NotRegisteredException;

public interface ICustodian extends Remote {
	
	//public ICustodianSession authenticate(String userId,String password) throws RemoteException,NotRegisteredException,AuthenticationFailedException;
	public ICustodianSession authenticate(String userId,String password, String userNode, String controlIP) throws RemoteException,NotRegisteredException,AuthenticationFailedException;
	public boolean register(String userId,String password) throws RemoteException,AlreadyRegisteredException;
	public boolean register_custodian(String userId) throws RemoteException,NotRegisteredException;
	public List<Integer> getUploadAcks(String contentName, int totalSegments) throws RemoteException;
	public void infoIP(String userId, String string) throws RemoteException;
	public boolean new_registration(Map<String, String> userInfo) throws RemoteException;

	
	/*
	
	public boolean request_connection(String user) throws RemoteException/*,UserAlreadyConnectedException,UserNotRegisteredException;
	public boolean register(String userId) throws RemoteException;
	public boolean unregister(String userId) throws RemoteException;
	public boolean subscribe(String subject) throws RemoteException;
	public boolean unsubscribe(String subject) throws RemoteException;
	public boolean close_connection() throws RemoteException;
	public Map<String,byte[]> request_data() throws RemoteException;
	//public Map<String,byte[]> request_data() throws RemoteException;
	public boolean find(String dataname,String user) throws RemoteException;
	*/

}

package prototype.user;

import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.List;
import java.util.Scanner;
import newNetwork.Connection;

public class Client
{
	public static void main(String[] args)
	{

		String host = new String("localhost");
		String username ="";
		String password ;
		String ch = "";
		String contentId ="";
		IUser stub ;
		Scanner in = new Scanner(System.in);
				
		try
		{
			
			/*********************User Login*********************************************/
			
			boolean userAuth = false ;
			while(!userAuth)
			{	
				boolean validUserName = false ;
				while(!validUserName)
				{	
					System.out.println("Enter the username: ") ;
					username = in.nextLine();
				
					if(username.length()>0)
						validUserName = true ;
					else
						System.out.println("You have entered invalid username. Please Try again");
				}	
				System.out.println("Enter the password: ") ;
				password = in.nextLine();
				stub = stubRMI(host); 
				String userID = stub.login(username,password);
				if(userID != null)
					userAuth= true ;
			}								
			/*******************Uploading Files**********************/	
			do{
				System.out.println("Requesting for upload in Client.java");
				System.out.println("Enter the File name to be uploaded:") ;
				boolean validFileName = false ;
				String name="" ;
			
				while(!validFileName)
				{	
					name = in.nextLine();
					if(name.length()>0)
						validFileName = true ;
					else
						System.out.println("You have enttered a wrong file Name");
				}	
			
				stub = stubRMI(host);
				int appId = stub.getAppId();
				try{
					contentId = stub.upload(name,Connection.Type.USB,appId,"youtube.com",username);
				}catch(Exception ex){
					ex.printStackTrace();
					System.out.println("ContentId not generated ");
				}
				System.out.println("ContentId generated is : "+contentId);
				System.out.println("Want to uploadmore files?, Press 'y' if yes: ");
				ch = in.nextLine();
			}while(ch.equals("y"));
			List<String> uploadList = null;
			boolean execute = true;
			
			while(execute)
			{
				/*********To check*******/
				try
				{
					uploadList = stub.getUploadList();
					for(int i = 0 ; i < uploadList.size() ;)
					{
						if(contentId.equals(uploadList.get(i++)))
						{
							execute = false ;
							break ;
						}   
					}
					Thread.sleep(1000);
				}
				catch(Exception e)
				{
					Thread.sleep(10000);
					stub = stubRMI(host);
				}
			} 
			System.out.println("The following files have been uploaded: "+uploadList.toString());
			
			//stub.find(contentId,Connection.Type.DSL,1); 
			stub.find(contentId,Connection.Type.DSL,1,username);
			System.out.println("Request for download has been sent");
			try 
			{	
				Thread.currentThread();
				Thread.sleep(3*600000);
				stub.logout(username);	
				System.out.println("logged Out");

			}
			catch (Exception e)
			{
				System.err.println("Client exception: " + e.getMessage());
				e.printStackTrace();
			}/**/
			
		} 
		catch (Exception e)
		{
			System.err.println("Client exception: " + e.toString());
			e.printStackTrace();
		}
	}
	
	/*****************Method for finding UserDaemon******************/
	public static IUser stubRMI(String host)throws Exception
	{
		Registry registry = LocateRegistry.getRegistry(host);
		IUser stub = null; 
		
		System.out.println("Finding UserDaemon.");
		boolean bound = false;
		while(!bound)
		{
			try
			{
				stub = (IUser) registry.lookup("userdaemon");
				bound = true;
			}
			catch(Exception ex)
			{
				Thread.sleep(1000);
			}
		}
		System.out.println("UserDaemon Found.");
		return stub ;
	}
}
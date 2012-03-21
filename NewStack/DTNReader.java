package NewStack;

import java.io.*;
import java.util.*;
import java.util.concurrent.BlockingQueue;

//import javax.swing.*;

import StateManagement.ContentState;
import StateManagement.StateManager;
import StateManagement.Status;
import prototype.datastore.DataStore;

public class DTNReader extends Thread {
	
	List<String> readingList ;
	DataStore store ;
	StateManager stateManager;
	int segmentSize;
	private BlockingQueue<String> fileDownloads;
	Map<String, ContentState> mpContent;
	String[] letters = new String[]{ "A", "B", "C", "D", "E", "F", "G", "H", "I", 
            "J", "K", "L", "M", "N", "O", "P", "Q", "R",
            "S", "T", "U", "V", "W", "X", "Y", "Z" };
	File[] drives = new File[letters.length];
	boolean[] isDrive = new boolean[letters.length];
	
	public DTNReader(List<String> dtnReadList, StateManager stmgr,DataStore dstore, int segment, BlockingQueue<String> downloads){
		readingList = dtnReadList ;
		store = dstore;
		stateManager = stmgr ;
		segmentSize = segment ;
		fileDownloads = downloads ;
		mpContent = new HashMap<String, ContentState>();
		for (int i = 0; i < letters.length; ++i )
	    {
	        drives[i] = new File(letters[i]+":/");
	        isDrive[i] = drives[i].canRead();
	    }
	}

	public void run(){
		while(true)
	    {
			try {
				Thread.sleep(2000);
			} catch (InterruptedException e2) {
				e2.printStackTrace();
			}
	        for (int i = 0; i < letters.length; ++i){
	        	//int ln = letters.length;
	        	//System.out.println("Inside NewStack.DTNReader: length:" + ln);
	            boolean pluggedIn = drives[i].canRead();
	            if (pluggedIn != isDrive[i]){
	                if(pluggedIn)
	                {	                
	                    System.out.println("Inside NewStack.DTNReader: Drive "+letters[i]+" has been plugged in");
	                    String str = "cmd /c \"dir "+ letters[i] + ":\"";
	                    try{
							Process process =Runtime.getRuntime().exec(str);
							BufferedReader input = new BufferedReader(new InputStreamReader(process.getInputStream()));
							boolean file = false;
							if(findInUSB(input,file,"DTNRouter"))
							{
								System.out.println("Inside NewStack.DTNReader: The USB key is a DTNRouter.");
								System.out.println("Inside NewStack.DTNReader: Block 1");//block 1
							}
							else{
								System.out.println("Inside NewStack.DTNReader: The USB key is not a DTNRouter.");
								continue;
							}
							//File drivePath = null;
							String filePathStr;
							for(int j=0;j<readingList.size();j++)
							{
								filePathStr = letters[i] + ":\\DTNRouter\\"+readingList.remove(0);
								System.out.println("Inside NewStack.DTNReader: File Name is: "+filePathStr);
								if(dtnFileRead(filePathStr))
									System.out.println("Inside NewStack.DTNReader: File is successfully read");
							}
							//System.out.println("Remove the USB drive");
							//JOptionPane.showMessageDialog(null,"Safely remove the drive");
							//JFrame parent = new JFrame();

						    //JOptionPane.showMessageDialog(parent, "Safely remove the drive");
						} 
		                catch (IOException e) 
		                {
							System.out.println("Inside NewStack.DTNReader: Error in reading or writing file");
							try {
								Thread.sleep(5000);
							} catch (InterruptedException e1) {
								e1.printStackTrace();
							}
						}
	                }
	                else
	                {
	                    System.out.println("Inside NewStack.DTNReader: Drive "+letters[i]+" has been unplugged");
	                }
	                isDrive[i] = pluggedIn;
	            }
	        }
	
	        // wait before looping
	        /*try { Thread.sleep(10000); }
	        catch (InterruptedException e) { /* do nothing  }*/
	
	    }
	}
	

	private  boolean findInUSB(BufferedReader input,boolean file,String name) throws IOException
	{
		String line = "";
		do
		{
			line = input.readLine();			
			if(line == null)
			{
				break;
			}
			if(line.contains("DTNRouter"))
			{
				if(line.contains("<DIR>"))
				{					
					return true;
				}				
			}		
		}while(line!=null);						
	
		return false;    	
	}
	
	private boolean dtnFileRead(String filePathStr){
		boolean flag = false ;
		File fileRead = new File(filePathStr) ;
		if(fileRead.exists()){
			ObjectInputStream objectIn = null ;
			try{
				objectIn = new ObjectInputStream(new FileInputStream(filePathStr)) ;
				while(true){
					Packet packet = (Packet) objectIn.readObject();
					mpContent = StateManager.getDownMap();
					String data = packet.getName();
					int offset = packet.getSequenceNumber();
					byte[] segment = packet.getData();
					store.write(data, offset, segment);
					
					ContentState conProp = mpContent.get(data);
					BitSet bs = conProp.bitMap;
					if(stateManager != null)
					{
						try	
						{
							int currentsegments = conProp.currentSegments;
							if(currentsegments == conProp.getTotalSegments()){
								
							}
							else
							{
								if(bs.get((int) (offset/segmentSize))==false)
								{
									currentsegments++;
									bs.set((int) (offset/segmentSize));
								}
								conProp.currentSegments = currentsegments;
								conProp.bitMap = bs ;
								mpContent.put(data,conProp);
								if(currentsegments == conProp.getTotalSegments())
								{
									System.out.println("Inside NewStack.DTNReader: In Reassembler.java Received the Complete File! :D :D :D :D :D :D :D :D :D :D :D :D ");
									Status st = Status.getStatus();
									if(fileDownloads != null)
										fileDownloads.put(data);
									st.updateState("status",data, 0); 
									
								}
							}
						}catch(Exception e){ 
							e.printStackTrace();
						}
					}
					
				}
				
			}catch(EOFException e){
				
			}
			catch(Exception e){
				e.printStackTrace();
			}
			finally{
				try{
					if(objectIn != null){
						objectIn.close();
						flag = true ;
					}	
				}catch(IOException e){
					e.printStackTrace();
				}
			}
		}
		return flag;
	}
}

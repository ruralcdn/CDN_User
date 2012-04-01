package newNetwork;

import java.io.*;
import java.net.InetAddress;
import java.util.concurrent.BlockingQueue;
//import NewStack.LinkDetector;


import NewStack.Packet;


public class USBConnection implements Connection{

	private Connection.Type type;
	private BlockingQueue<Packet> readQueue;    //reads from USB
	@SuppressWarnings("unused")
	private BlockingQueue<Packet> writeQueue;   //writes to USB
	private String dtnStore ;
	//inQueue - reads from USB , outQueue - writes to USB
	public USBConnection(BlockingQueue<Packet> inQueue,BlockingQueue<Packet> outQueue){

		type = Connection.Type.USB;
		readQueue = inQueue;
		writeQueue = outQueue; 
	}

	public USBConnection(String dtnDir) {
		type = Connection.Type.USB;
		dtnStore = dtnDir ;
	}

	public Connection.Type getType()
	{
		return type;
	}	
	public Packet readPacket()
	{
		return readQueue.poll();
	}
	public void writePacket(Packet packet)
	{
		System.out.println("Inside newNetwork.USBConnection: Method writePacket");
		String fileName = packet.getName();
		String filePath = dtnStore+fileName ;
		//ObjectOutputStream out = null;
		
		File file = new File(filePath);
		
		/*try{
		if(!file.exists())
			out =  new ObjectOutputStream(new FileOutputStream (filePath));
		else
			out = new AppendableObjectOutputStream (new FileOutputStream (filePath, true));
		out.writeObject(packet);
		long filesize = (file.length()/(1024));
		//int check =LinkDetector.
		System.err.println("File Size Is: "+filesize+"KB");
		
        out.flush ();
		}
		catch(Exception e){
			e.printStackTrace();
		}*/
		/*Code by gauravluthra06@gmail.com*/
		 
		 	FileOutputStream fout = null;
		try{
			if(!file.exists())
				fout = new FileOutputStream (filePath);
			else
				fout = new FileOutputStream (filePath, true);
			fout.write(packet.getBytePacket());
	        fout.flush ();
	        //System.err.println("remove your usb drive first");
			}
			catch(Exception e){
				e.printStackTrace();
			}
		
		finally{
            try{
                if (fout != null) fout.close ();
               // System.err.println("remove your usb drive second");
            }catch (Exception e){
                e.printStackTrace ();
            }
        }
		/*finally{
            try{
                if (out != null) out.close ();
            }catch (Exception e){
                e.printStackTrace ();
            }
        }*/
		//writeQueue.offer(packet);
		//System.err.println("remove your usb drive third");
	}

	public void writePacketNewMethod(Packet packet)
	{
		System.out.println("Inside newNetwork.USBConnection: level 2");
		String fileName = packet.getName();
		String filePath = dtnStore+fileName ;
		ObjectOutputStream out = null;
		File file = new File(filePath);
		try{
		if(!file.exists())
			out =  new ObjectOutputStream(new FileOutputStream (filePath));
		else
			out = new AppendableObjectOutputStream (new FileOutputStream (filePath, true));
		out.writeObject(packet);
        out.flush ();
		}
		catch(Exception e){
			e.printStackTrace();
		}
		finally{
            try{
                if (out != null) out.close ();
            }catch (Exception e){
                e.printStackTrace ();
            }
        }
		//writeQueue.offer(packet);		
		
	}
	public void close() throws IOException
	{

	}

	public InputStream getInputStream() {
		// TODO Auto-generated method stub
		return null;
	}

	public OutputStream getOutputStream() {
		// TODO Auto-generated method stub
		return null;
	}
	
	public String getRemoteAddress()
	{
		return null;
	}
	public int getRemotePort()
	{
		return -1;
	}
	
	public InetAddress getLocalAddress()
	{
		return null;
	}
	public int getLocalPort()
	{
		return -1;
	}
}

class AppendableObjectOutputStream extends ObjectOutputStream {
    public AppendableObjectOutputStream(OutputStream out) throws IOException {
      super(out);
    }
    @Override
    protected void writeStreamHeader() throws IOException {}
}
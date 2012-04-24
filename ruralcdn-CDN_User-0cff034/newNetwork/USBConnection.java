package newNetwork;

import java.awt.Image;
import java.awt.SystemTray;
import java.awt.Toolkit;
import java.awt.TrayIcon;
import java.io.*;
import java.net.InetAddress;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.concurrent.BlockingQueue;
import NewStack.Packet;

public class USBConnection implements Connection{	
	int count = 0;
	private Connection.Type type;
	private BlockingQueue<Packet> readQueue;    //reads from USB
	@SuppressWarnings("unused")
	private BlockingQueue<Packet> writeQueue;   //writes to USB
	private String dtnStore ;
	//inQueue - reads from USB , outQueue - writes to USB
	public int chk;	
	
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
	public void writePacket(Packet packet){	

		//getsegments();

		//User1 frame = new User1();

		//System.out.println("Inside newNetwork.USBConnection: Method writePacket");
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
		//long filesize = (file.length()/(1024));
		//int check =LinkDetector.
		//System.err.println("File Size Is: "+filesize+"KB");
		System.err.println("Copying.......");

        out.flush ();
		}
		catch(Exception e){
			e.printStackTrace();
		}
		/*Code by gauravluthra06@gmail.com*/

		/*FileOutputStream fout = null;
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
				if (fout != null){
					fout.close ();}
				else{
					System.err.println("remove your usb drive second");
				}
				
			}catch (Exception e){
				e.printStackTrace ();
			}
		}*/
		finally{
            try{
                if (out != null) out.close ();
            }catch (Exception e){
                e.printStackTrace ();
            }
        }
		//writeQueue.offer(packet);
		//System.err.println("remove your usb drive third");

	}

	@SuppressWarnings("unused")
	private void getsegments() {
		java.sql.Connection con = null ;
		java.sql.Statement stat ;
		java.sql.PreparedStatement prep;
		String table = "segments";
		java.sql.ResultSet resset = null ;
		int num = 0;
		
		try{
			Class.forName("com.mysql.jdbc.Driver");
		}catch(ClassNotFoundException e){
			e.printStackTrace();

		}

		try {
			con = DriverManager.getConnection
			("jdbc:mysql://localhost:3306/ruralcdn","root","abc123");
		} catch (SQLException e) {
			e.printStackTrace();
		}
		
		try {
			stat = con.createStatement();
			stat.execute("select * from "+table);
			resset = stat.getResultSet();
			while(resset.next()){
				num = resset.getInt(1);
			}
			resset.close();
			stat.close();
		} catch (Exception e) {
			//e.printStackTrace();
			System.out.println("number value"+num);
		}
		count ++;
		if(count >= num){
			System.err.println("safely Remove USB Drive");
			try{

				prep= con.prepareStatement("delete from "+table);				
				prep.execute();
				prep.close();
			}catch(Exception e){
				//e.printStackTrace();
				System.out.println("Exception occurs at updateState");
			}
			
			Toolkit toolkit = Toolkit.getDefaultToolkit();
			Image image = toolkit.getImage("E:/logo_usb.jpg");

			TrayIcon trayIcon = new TrayIcon(image, "trayicon");

			SystemTray tray = SystemTray.getSystemTray();

			try
			{
				tray.add(trayIcon);
			}
			catch (Exception e)
			{
				System.out.println("TrayIcon could not be added.");												
			}

			trayIcon.displayMessage("Safely Remove USB Drive", "Please Safely Remove USB Drive", TrayIcon.MessageType.INFO);

			
		}else{
			System.err.println("copying....");
		}
		
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
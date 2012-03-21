package prototype.utils;

import java.io.File;
import java.io.FileOutputStream;
import java.util.concurrent.ArrayBlockingQueue;

public class FileWriter extends Thread{
	
	File file;
	ArrayBlockingQueue<byte[]> buffer;
	boolean flag;
	
	   public FileWriter(File f,ArrayBlockingQueue<byte[]> b,boolean done){
	    	this.file = f;
	    	this.buffer = b;
	    	this.flag = done;
	    }
	
	   public void run(){
		   System.out.println("Inside prototype.utils.FileWriter:");
		   while(buffer.peek()!= null || !flag)
		   {
		   byte[] bytes = buffer.poll();
		   try
		   {
		   FileOutputStream fos = new FileOutputStream(file,true);
		   fos.write(bytes);
		   fos.close();
		   }catch(Exception e)
		   {
			   System.out.println("Inside FileWriter:FileNotFoundException: "+e.getMessage());
		   }
		   }
		   
	   }
	
}
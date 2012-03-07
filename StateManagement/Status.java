package StateManagement ;

import java.sql.*;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
public class Status{
	public String contentId;
	public int type;
	public int totseg;
	public int curseg;
	public int off ;
	public int prefint;
	public String prefrt;
	public String appid;
	public int sendmetadata;
	public String prefrtport;
	String[] st = new String[8];
	Connection con ;
	Statement stat ;
	PreparedStatement prep;
	private static Status myStatus;

	private  Status(){

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
	}	

	public static synchronized Status getStatus(){
		if(myStatus==null)
			myStatus = new Status();
		return myStatus;
	}

	public void insertData(String table, String contentId, int type, int totseg, int curseg, int off, int prefint, String prefrt, String appid, int sendmetadata, String prefrtport ){

		try{
			//System.out.println("table name:"+table);
			prep = con.prepareStatement("insert into "+table+" values(?,?,?,?,?,?,?,?,?,?)");
			prep.setString(1,contentId);
			prep.setInt(2,type);
			prep.setInt(3,totseg);
			prep.setInt(4,curseg);
			prep.setInt(5,off);
			prep.setInt(6,prefint);
			prep.setString(7,prefrt);
			prep.setString(8,appid);
			prep.setInt(9,sendmetadata);
			prep.setString(10,prefrtport);
			prep.execute();
			prep.close();

		}catch(Exception e){
			e.printStackTrace();
			System.out.println("Exception occurs at insertData ");
		}
	}

	public void insertData(String table, String dataServerId, String cacheServerId ){

		try{
			prep = con.prepareStatement("insert into "+table+" values(?,?,?)");
			prep.setString(1,dataServerId);
			prep.setString(2,cacheServerId);
			prep.setBoolean(3,false);
			prep.execute();
			prep.close();

		}catch(Exception e){
			//e.printStackTrace();
			System.out.println("Exception occurs at insertData2");
		}
	}
	public boolean execQuery(String s,String contentId, int type){
		boolean flag = false;
		ResultSet resset = null ;
		try{
			stat = con.createStatement();
			stat.execute(s);
			resset = stat.getResultSet();
			while(resset.next()){
				String str = resset.getString("contentid");
				int t = resset.getInt("type");
				if(str.equals(contentId) && t== type)
				{
					flag = true ;
					break ;
				}	
			}
			resset.close();
			stat.close();
		}catch(Exception e){
			//e.printStackTrace();
			System.out.println("Exception occurs at execQuery");
		}
		return flag ;
	}
	public String[] execQuery(String s){
		ResultSet resset = null ;
		st[0] = "";
		st[1] = "";
		st[2] = "";
		st[3] = "";
		st[4] = "";
		st[5] = "";
		st[6] = "";
		st[7] = "";
		try{
			stat = con.createStatement();
			stat.execute(s);
			resset = stat.getResultSet();
			if(resset.next()){
				st[0] = Integer.toString(resset.getInt("off") );
				st[1] = Integer.toString(resset.getInt("prefint") );
				st[2] = Integer.toString(resset.getInt("totseg")) ;
				st[3] = resset.getString("appid");
				st[4] = Integer.toString(resset.getInt("sendmetadata"));
				st[5] = resset.getString("prefrt");
				st[6] = resset.getString("prefrtport");
				st[7] = Integer.toString(resset.getInt("curseg")) ;

			}	
			resset.close();
			stat.close();
		}catch(Exception e){
			//e.printStackTrace();
			System.out.println("Exception occurs at execQuery2");
		}		
		return st ;
	}	

	public void execQuery(String table, String contentId){
		try {
			stat = con.createStatement();
			stat.execute("update "+table+" set flag = '1' where dsid = '"+contentId+"'");
			stat.close();
		} catch (Exception e) {
			System.out.println("Exception occurs at execeQuery3");
			//e.printStackTrace();
		}


	}
	public List<String> execQuery(String table, boolean status){
		List<String> uploadedList = new ArrayList<String>();
		ResultSet resset = null ;
		try {
			stat = con.createStatement();
			stat.execute("select * from "+table+" where flag = '1'");
			resset = stat.getResultSet();
			while(resset.next()){
				uploadedList.add(resset.getString("dsid"));
			}
			resset.close();
			stat.close();
		} catch (Exception e) {
			//e.printStackTrace();
			System.out.println("Exception occurs at execquery3");
		}
		return uploadedList;
	}
	public void updateData(String table){

		try{

			prep= con.prepareStatement("update "+table+" set totseg = ?, curseg = ?, off = ?," +
					" prefint = ?, prefrt = ?, appid = ?, sendmetadata = ?, prefrtport = ? where contentid = ? " +
			"and type = ?");
			prep.setInt(1,this.totseg);
			prep.setInt(2,curseg);
			prep.setInt(3,this.off);
			prep.setInt(4,this.prefint);
			prep.setString(5,this.prefrt);
			prep.setString(6,this.appid);
			prep.setInt(7,this.sendmetadata);
			prep.setString(8,this.prefrtport);
			prep.setString(9,this.contentId);
			prep.setInt(10,this.type);
			prep.execute();
			prep.close();


		}catch(Exception e){
			//e.printStackTrace();
			System.out.println("Exception occurs at updateData");
		}	
	}	

	public void updateState(String table, String contentId, int type, int size){

		try{

			prep= con.prepareStatement("update "+table+" set curseg = ? where contentid = ? " +
			"and type = ?");
			prep.setInt(1,size);
			prep.setString(2,contentId);
			prep.setInt(3,type);
			prep.execute();
			prep.close();
		}catch(Exception e){
			System.out.println("Exception occurs at updateState");
		}

	}

	public void updateState(String table, String contentId, int type){

		try{

			prep= con.prepareStatement("delete from "+table+" where contentid = ? " +
			"and type = ?");
			prep.setString(1,contentId);
			prep.setInt(2,type);
			prep.execute();
			prep.close();
		}catch(Exception e){
			//e.printStackTrace();
			System.out.println("Exception occurs at updateState");
		}

	}

	public Map<String,String> setUploadRequsets(String table, String contentId, String uploadId){
		Map<String,String> uploadRequestList = new HashMap<String,String>();
		try{
			System.out.println("Table Name = " + table);
			prep = con.prepareStatement("insert into "+table+" values(?,?) ");
			//System.out.println("table name 2:"+table);
			//System.out.println("contentId & uploadId"+" "+contentId+" "+uploadId);
			prep.setString(1,contentId);
			prep.setString(2,uploadId);
			
			
			prep.execute();
			
			
			prep.close();
			stat = con.createStatement();
			stat.execute("select * from "+table);
			ResultSet resset = stat.getResultSet();
			while(resset.next()){
				uploadRequestList.put(resset.getString("contentid"),resset.getString("uploadId"));
			}
			resset.close();
			stat.close();
		}catch(Exception e){
			e.printStackTrace();
			System.out.println("Exception occurs at Status:setUploadRequests");
		}
		return uploadRequestList ;
	}

	public Map<String,String> setUploadRequsets(String table, String contentId){
		Map<String,String> uploadRequestList = new HashMap<String,String>();
		try{

			prep = con.prepareStatement("delete from "+table+" where contentid = ? ");
			prep.setString(1,contentId);
			prep.execute();
			prep.close();
			stat = con.createStatement();
			stat.execute("select * from "+table);
			ResultSet resset = stat.getResultSet();
			while(resset.next()){
				uploadRequestList.put(resset.getString("contentid"),resset.getString("uploadId"));
			}
			resset.close();
			stat.close();
		}catch(Exception e){
			e.printStackTrace();
			System.out.println("Exception occurs at setUploadRequest");
		}
		return uploadRequestList ;
	}
	public Map<String,String> getUploadRequsets(String table){
		Map<String,String> uploadRequestMap = new HashMap<String,String>();
		try{
			stat = con.createStatement();
			stat.execute("select * from "+table);
			ResultSet resset = stat.getResultSet();
			while(resset.next()){
				uploadRequestMap.put(resset.getString("contentid"),resset.getString("uploadid"));
			}
			resset.close();
			stat.close();
		}catch(Exception e){
			e.printStackTrace();
			System.out.println("Exception occurs at getUploadRequests");
		}
		return uploadRequestMap ;
	}

	public Map<String,ContentState> getPendingStatus(String query)
	{
		Map<String, ContentState> map = new HashMap<String, ContentState>();
		try
		{
			stat =  con.createStatement();
			stat.execute(query);
			ResultSet resset = stat.getResultSet();
			while(resset.next())
			{
				String contentId = resset.getString("contentId");
				int off = resset.getInt("off");
				int size = resset.getInt("totseg");
				BitSet bitSet = new BitSet(size);
				int prefInt = resset.getInt("prefint");
				String appId = resset.getString("appid");
				boolean meta = resset.getBoolean("sendmetadata");
				ContentState contentState = new ContentState(contentId,off,bitSet,prefInt,null,size,0,ContentState.Type.tcpDownload,appId,meta);
				map.put(contentId,contentState);
			}
			resset.close();
			stat.close();
		} catch (Exception e)
		{
			e.printStackTrace();
			System.out.println("Exception occurs at getPendingStatus");
		}
		return map ;
	}

	public boolean executeQuery(String str){
		boolean flag = false ;
		ResultSet resset = null ;
		try
		{
			Statement stat =  con.createStatement();
			stat.execute(str);
			resset = stat.getResultSet();
			if(resset.next())
				flag = true ;
			resset.close();
			stat.close();
		}
		catch(Exception e){
			System.out.println("Value of flag in executeQuery(String str) is :"+flag);
			
		}
		return flag;	

	}
	
	public List<String> getThumbData(){
		List<String> imgList = new ArrayList<String>();
		ResultSet rs = null ;
		try{
			
			Statement stmt = con.createStatement();
			stmt.execute("select dbvalue from dbsync where dbvalue like '%jpg'");
			rs = stmt.getResultSet();
			while(rs.next()){
				String content = rs.getString("dbvalue");
				imgList.add(content);
			}
		}catch(Exception e){
			e.printStackTrace();
		}
		return imgList ;
	}
}

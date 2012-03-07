package StateManagement;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

public class ApplicationStateManager {

	private static final String userUploadIdSuffix = new String(".UserUploadId");
	File status;
    public ApplicationStateManager(File state)
	{
		status = state;	
	}

    public void setServiceUploadName(String uploadId,String serviceUploadId, String fileType)
	{
		Status st = Status.getStatus();
		String type = "."+fileType;
		st.insertData("uploadedItem", serviceUploadId+type, uploadId+type);
	}
	
	public String getUserUploadId(String serviceUploadId)
	{
		Properties state = new Properties();
		FileInputStream fis;
		
		try {
			fis = new FileInputStream(status);
			state.load(fis);
			fis.close();
		    return state.getProperty(serviceUploadId+userUploadIdSuffix);
		
		} catch (FileNotFoundException e) {
			e.printStackTrace();
			return null;
		}catch(IOException e){
			e.printStackTrace();
			return null;
		}
	}
    
   	public List<String> getUploadAcks(){
   		List<String> uploadedList = new ArrayList<String>();
   		Status st = Status.getStatus();
   		uploadedList=st.execQuery("uploadeditem", true);
		return uploadedList;
   		
   	}
	
	public void addUploadAcks(String contentId){
   		Status st = Status.getStatus();
   		st.execQuery("uploadeditem",contentId);
   		
   	}

}
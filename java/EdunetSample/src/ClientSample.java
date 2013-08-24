import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStreamReader;

import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;
import org.apache.http.entity.ContentType;
import org.apache.http.HttpResponse;
import org.json.simple.parser.JSONParser;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.ParseException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

import net.asdfa.msgpack.MsgPack;;


@SuppressWarnings("unchecked")
public class ClientSample {

    private String email = "admin@local.host";
    private String password = "changeme";
    private String apiURL = "http://edunet.herokuapp.com/";
    //private String apiURL = "http://localhost:3000/";


    public ClientSample() {        
    }
 
    private Object makeHTTPGETJSONRequest(String url) {
        try {
            HttpClient c = new DefaultHttpClient();        
            HttpGet p = new HttpGet(url);        
            HttpResponse r = c.execute(p);
            BufferedReader rd = new BufferedReader(new InputStreamReader(r.getEntity().getContent()));
            JSONParser parser = new JSONParser();
        	return parser.parse(rd);
        }
        catch(ParseException e) {
            System.out.println(e);
        }
        catch(IOException e) {
            System.out.println(e);
        }                        
        return null;
    }    
    
    private Object makeHTTPGETMsgPackRequest(String url) {
        try {
            HttpClient c = new DefaultHttpClient();        
            HttpGet p = new HttpGet(url);        
            HttpResponse r = c.execute(p);
            
            /*
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            byte[] tmp = new byte[4096];
            int ret = 0;
            while((ret = r.getEntity().getContent().read(tmp)) > 0) {
                bos.write(tmp, 0, ret);
            }
            byte[] myArray = bos.toByteArray();
            
            ByteArrayInputStream ins = new ByteArrayInputStream(myArray);
            return MsgPack.unpack(new DataInputStream(ins), MsgPack.UNPACK_RAW_AS_STRING);
            */
        	return MsgPack.unpack(new DataInputStream(new BufferedInputStream(r.getEntity().getContent(), 16384)), MsgPack.UNPACK_RAW_AS_STRING);
        }
        catch(IOException e) {
            System.out.println(e);
        }                        
        return null;
    }    

    private Object makeHTTPPOSTJSONRequest(String url, String body) {
        try {
            HttpClient c = new DefaultHttpClient();        
            HttpPost p = new HttpPost(url);        
            p.setEntity(new StringEntity(body, ContentType.create("application/json"))); 
            HttpResponse r = c.execute(p);
            if (r.getStatusLine().getStatusCode() == 204)
            	return null;
            BufferedReader rd = new BufferedReader(new InputStreamReader(r.getEntity().getContent()));
            JSONParser parser = new JSONParser();
        	return parser.parse(rd);
        }
        catch(ParseException e) {
            System.out.println(e);
        }
        catch(IOException e) {
            System.out.println(e);
        }                        
        return null;
    }    

    private Object makeHTTPPOSTMsgPackRequest(String url, byte[] body) {
        try {
            HttpClient c = new DefaultHttpClient();        
            HttpPost p = new HttpPost(url);        
            p.setEntity(new ByteArrayEntity(body, ContentType.create("application/x-mpac"))); 
            HttpResponse r = c.execute(p);
            if (r.getStatusLine().getStatusCode() == 204)
            	return null;
        	return MsgPack.unpack(new DataInputStream(r.getEntity().getContent()), MsgPack.UNPACK_RAW_AS_STRING);
        }
        catch(IOException e) {
            System.out.println(e);
        }                        
        return null;
    }    

    private String createToken() {
    	JSONObject obj=new JSONObject();
    	obj.put("email", email);
    	obj.put("password", password);    	  
    	String content = obj.toJSONString();
    	JSONObject res = (JSONObject)makeHTTPPOSTJSONRequest(apiURL + "token.json", content);
    	
    	return res.get("auth_token").toString();
    }

	@SuppressWarnings("rawtypes")
	private List listUsers(String token) {
		String url = apiURL + "users.mpac?auth_token=" + token;
		Object res = makeHTTPGETMsgPackRequest(url);
		return (List)res;
    }

	@SuppressWarnings("rawtypes")
	private List listStudyJournals(String token, long since) {
		String url = apiURL + "study_journals.mpac?auth_token=" + token + "&since="+since;
		Object res = makeHTTPGETMsgPackRequest(url);
		return (List)res;
    }

	private void createStudyJournal(String token) {
		java.util.Date date = new java.util.Date();
		long now = date.getTime();
		
		JSONArray list = new JSONArray();
		for  (int i = 0; i < 10; i++) {
	    	JSONObject obj=new JSONObject();
	    	obj.put("guid", UUID.randomUUID().toString());
	    	obj.put("course_id", "course0001");    	  
	    	obj.put("start_time", now - 60000);    	  
	    	obj.put("end_time", now);    	  
	    	obj.put("seconds", 60);    	  
	    	list.add(obj);
		}
    	JSONObject obj=new JSONObject();
    	obj.put("journals", list);
    	String content = obj.toJSONString();
    	makeHTTPPOSTJSONRequest(apiURL + "study_journals.json?auth_token=" + token, content);
    }

	@SuppressWarnings("rawtypes")
	private List listAnnotations(String token, long since) {
		String url = apiURL + "annotations.mpac?auth_token=" + token + "&since="+since;
		System.out.println(url);
		Object res = makeHTTPGETMsgPackRequest(url);
		return (List)res;
    }

	private byte[] getAnnotationContent(String token, String guid) throws IOException {
		String url = apiURL + "annotations/" + guid + ".content?auth_token=" + token;
        HttpClient c = new DefaultHttpClient();        
        HttpGet p = new HttpGet(url);        
        HttpResponse r = c.execute(p);
        return EntityUtils.toByteArray(r.getEntity());
    }

	@SuppressWarnings("rawtypes")
	private void createAnnotation(String token) {
		java.util.Date date = new java.util.Date();
		long now = date.getTime();
		
		ArrayList list = new ArrayList();
		HashMap attrs = new HashMap();
	    attrs.put("guid", UUID.randomUUID().toString());
	    attrs.put("course_id", "course0001");    	  
	    attrs.put("ctype", "image");    	  
	    byte[] b = new byte[1024];
	    new Random().nextBytes(b);
	    
	    attrs.put("content", b);

	    list.add(attrs);
	    HashMap obj = new HashMap();
    	obj.put("annotations", list);
    	makeHTTPPOSTMsgPackRequest(apiURL + "annotations.mpac?auth_token=" + token, MsgPack.pack(obj));
    }

	@SuppressWarnings("rawtypes")
	public static void main(String[] args) {
    	ClientSample sample = new ClientSample();
    	String token = sample.createToken();
    	System.out.println(token);
    	
    	// list all users
    	/*
		List ulist = sample.listUsers(token);
		for(Object it : ulist) {
			Map attrs = (Map)it;
    		System.out.println(attrs);
		}
		*/

		//sample.createStudyJournal(token);
    	//sample.createAnnotation(token);
    	
    	// Get annotations
    	boolean eof = false;
    	long since = 0;
    	while(!eof) {
    		List list = sample.listAnnotations(token, since);
    		for(Object it : list) {
    			Map attrs = (Map)it;
        		System.out.println(attrs);
        		since = (long)attrs.get("server_at");
        		String guid = (String)attrs.get("guid");
        		
        		// get content
        		try {
            		byte[] content = sample.getAnnotationContent(token, guid);
            		System.out.println(content.length);
        		} catch(IOException e) {        			
        		}
    		}
    		eof = list.size() == 0;
    		since++;
    	}

    	// Get study journals
    	eof = false;
    	since = 0;
    	while(!eof) {
    		List list = sample.listStudyJournals(token, since);
    		for(Object it : list) {
    			Map attrs = (Map)it;
        		System.out.println(attrs);
        		since = (long)attrs.get("server_at");
    		}
    		eof = list.size() == 0;
    		since++;
    	}    	

	}

}

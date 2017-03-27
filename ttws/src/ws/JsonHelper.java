package ws;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

public class JsonHelper {
	private String address;
	private String parameters;
	private URL url;
	private HttpURLConnection connection;
	private BufferedReader in;
	
	public JsonHelper(final String address) {
		this.address = address;
		this.parameters = this.address;
	}
	
	/**
	 * Sets parameters for a future URL request for JSON
	 * @param parameters each two parameters should be a couple of KEY and VALUE, example: "username" and "George"(username=George)
	 */
	public void setParameters(final String... parameters){
		int i = 0;
		
		//first two are without the '?' sign
		this.parameters = this.address.concat("?" + parameters[0] + "=" + parameters[1]);
		
		//start from 2, because first two parameters are already set
		for(i = 2; i < parameters.length; i += 2){
			
			// '&' is added at the start, since the last one will not need a '&' in the end
			this.parameters = this.parameters.concat("&" + parameters[i] + "=" + parameters[i+1]);
		}
	}
	
	/**
	 * Returns the JSON from the URL request
	 * @return the JSON string or if something went wrong, null
	 */
	public String getJson(){
		try {
			this.url = new URL(this.parameters);
			this.connection = (HttpURLConnection) this.url.openConnection();
			this.connection.setRequestMethod("GET");
			
			//GET method is authorized 
			if(this.connection.getResponseCode() == 200){
				this.in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
				String inputLine;
				StringBuffer response = new StringBuffer();
				while ((inputLine = in.readLine()) != null) {
					response.append(inputLine);
				}
				this.in.close();
				
				//return the JSON
				return response.toString();
			}
			
		} catch (MalformedURLException e) {
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		return null;
	}
	
	
}

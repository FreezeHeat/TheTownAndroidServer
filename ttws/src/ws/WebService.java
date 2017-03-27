package ws;

import java.lang.reflect.Type;

import java.util.List;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import db.DB;
import ben_and_asaf_ttp.thetownproject.shared_resources.Player;

@Path("/thetown")
public class WebService {
	private DB db = DB.getDB();
	private Gson gson = new Gson();
	
	@GET
	@Consumes(MediaType.TEXT_PLAIN)
	@Produces(MediaType.APPLICATION_JSON)
	@Path("/login")
	public String login(
			@QueryParam("player")final String player){
		Player p = (Player) gson.fromJson(player, Player.class);
		p = db.login(p);
		return (p == null) ? null : gson.toJson(p, Player.class);
	}
	
	@GET
	@Consumes(MediaType.TEXT_PLAIN)
	@Produces(MediaType.APPLICATION_JSON)
	@Path("/register")
	public String register(
			@QueryParam("player")final String player){
		Player p = gson.fromJson(player, Player.class);
		p = db.register(p);
		return (p == null) ? null : gson.toJson(p, Player.class);
	}
	
	@GET
	@Produces(MediaType.APPLICATION_JSON)
	@Path("/top10")
	public String getTop10(){
		Type playerListType = new TypeToken<List<Player>>(){}.getType();
		return gson.toJson(db.getTop10Players(), playerListType);
	}
	
	@GET
	@Consumes(MediaType.TEXT_PLAIN)
	@Produces(MediaType.APPLICATION_JSON)
	@Path("/searchPlayer")
	public String searchPlayer(
			@QueryParam("username")final String username){
		Player p = db.searchPlayer(username);
		return (p == null) ? null : gson.toJson(p, Player.class);
	}
}

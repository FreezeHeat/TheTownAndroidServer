<%@page import="com.google.gson.GsonBuilder"%>
<%@page import="com.google.gson.reflect.TypeToken"%>
<%@page import="java.lang.reflect.Type"%>
<%@page import="ws.JsonHelper"%>
<%@page import="com.google.gson.Gson"%>
<%@page import="ben_and_asaf_ttp.thetownproject.shared_resources.Player"%>
<%@page import="java.util.*"%>
<%@page import="db.DB"%>
<%@ page language="java" contentType="text/html; charset=UTF-8"
    pageEncoding="UTF-8"%>

<!DOCTYPE html PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN"
    "http://www.w3.org/TR/html4/loose.dtd">
<html>
<head>
<meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
<meta name="viewport" content="width=device-width, initial-scale=1">
<link rel="stylesheet" href="bootstrap-3.3.7-dist/css/bootstrap.min.css">
<link rel="stylesheet" href="style.css">
<title>Search Users</title>
</head>
<body>
	<%!
	Object user = null;
	String username = null;
	String password = null;
	String message;
	
	//URL connection to WebService
	JsonHelper json = new JsonHelper("http://localhost:8080/TTWS/rest/thetown/searchPlayer");
	JsonHelper jsonTop10 = new JsonHelper("http://localhost:8080/TTWS/rest/thetown/top10");
	Gson gson;
	%>
	
	<header class="container-fluid">
		<div class="row">
			<img src="pics/login.png" class="img-responsive centerBlock logo" alt="Responsive image">
		</div>
	</header>
	
	<div class="spaceBlockSmall"></div>

	<div class="container whiteText">
		<div class="row">
			<div class="centerBlock" id="searchUser">
		
				<% 
				gson = new GsonBuilder().excludeFieldsWithoutExposeAnnotation().create();
				user = request.getParameter("username");
				if(user != null){
					username = user.toString();
				}
				%>
				
				<p class="align-center"><strong>Enter username to search for.</strong></p>
				<form class="form-inline">
					<div class="form-group">
						<input class="form-control" type="text" name="username">
						<input class="btn btn-primary btn-lg" type="submit" value="SEARCH!">
					</div>
				</form>
			
				<%
				if(username != null && username != ""){
					json.setParameters("username", username);
					String jsonResponse = json.getJson();
					Player p = (Player)gson.fromJson(jsonResponse, Player.class);
					if(p != null){
				%>		<div class="table-responsive">
						<table class="table table-bordered tableBackground">
							<tr>
							    <th>Name</th> 
							    <th>Won</th>
							    <th>Lost</th>
							    <th>Kills</th>
							    <th>Heals</th>
							</tr>
							<tr>
							    <td><%= p.getUsername() %></td> 
							    <td><%= p.getStats().getWon() %></td>
							    <td><%= p.getStats().getLost() %></td>
							    <td><%= p.getStats().getKills() %></td>
							    <td><%= p.getStats().getHeals() %></td>
							</tr>
						</table>
						</div>
				<% 
					}else{
				%>
					<strong>No such user</strong>
				<% 
					}
				}else{
				%>
				
					<strong>Wrong username!</strong>
				
				<%
				}
				%>
			</div>
			<div class="spaceBlock"></div>
			</div>
			
			<div class="row">
				<div class="centerBlock" id="top10">
						<form>
							<input type="hidden" name="top10" value="1">
							<input type="submit" class="btn btn-primary btn-lg" value="Show top 10 players">
						</form>
						
						<%
						if(request.getParameter("top10") != null){
							String jsonResponse = jsonTop10.getJson();
						
							//Look in "WebService.java" at /top10 path for more information about why Type was used
							Type playerListType = new TypeToken<List<Player>>(){}.getType();
							List<Player> topPlayers = (jsonResponse != null) ? (List<Player>)gson.fromJson(jsonResponse, playerListType) : null;
							if(topPlayers != null){
								int i=1;
						%>
					
						<div class="table-responsive">
						<table class="table table-bordered tableBackground">
							<tr>
							    <th>Place</th>
							    <th>Name</th> 
							    <th>Rating</th>
							    <th>Won</th>
							    <th>Lost</th>
							    <th>Kills</th>
							    <th>Heals</th>
							</tr>
					  
						<% 		for(Player p : topPlayers){   %>
							<tr>
							    <td><%= i++ %></td>
							    <td><%= p.getUsername() %></td> 
							    <td><%= p.getStats().getRating() %>
							    <td><%= p.getStats().getWon() %></td>
							    <td><%= p.getStats().getLost() %></td>
							    <td><%= p.getStats().getKills() %></td>
							    <td><%= p.getStats().getHeals() %></td>
							</tr>
						<% 
								}
							}
						}
					%>
					
					</table>
					</div>
				</div>
			</div>
		</div>

</body>
</html>
<%@page import="com.google.gson.GsonBuilder"%>
<%@page import="com.google.gson.Gson"%>
<%@page import="ws.JsonHelper"%>
<%@page import="ben_and_asaf_ttp.thetownproject.shared_resources.Commands"%>
<%@page import="ben_and_asaf_ttp.thetownproject.shared_resources.Player"%>
<%@page import="db.DB"%>
<%@ page language="java" contentType="text/html; charset=utf8"
    pageEncoding="utf8"%>
<!DOCTYPE html PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">
<html>
<head>
<meta http-equiv="Content-Type" content="text/html; charset=utf8">
<meta name="viewport" content="width=device-width, initial-scale=1">
<link rel="stylesheet" href="bootstrap-3.3.7-dist/css/bootstrap.min.css">
<link rel="stylesheet" href="style.css">
<title>Login page</title>
</head>
<body>
<%!
	String username = null;
	String password = null;
	Object user = null;
	Object pass = null;
	
	//Error message for user
	String message;
			
	//URL connection to WebService
	JsonHelper json = new JsonHelper("http://localhost:8080/TTWS/rest/thetown/login");
	Gson gson;
%>

<header class="container-fluid">
	<div class="row">
		<img src="pics/login.png" class="img-responsive centerBlock logo" alt="Responsive image">
	</div>
</header>

<div class="container">
	<div class="row">
		<%
		gson = new GsonBuilder().excludeFieldsWithoutExposeAnnotation().create();
		
		//Get info from session (if not blank)
		user = session.getAttribute("username");
		pass = session.getAttribute("password");
		
		if(user != null && pass != null){
			username = user.toString(); 
			password = pass.toString();
		}
		
		//if there's no session
		if(username == null || password == null){
			
			//check paramters (from Html Form)
			user = request.getParameter("username");
			pass = request.getParameter("password");
			
			if( (user != null && pass != null)){
				if(user.toString() != "" && pass.toString() != ""){
					username = user.toString(); 
					password = pass.toString();
				}
			}
			
			//if these are blank as well, show login form
			if(username == null ||  password == null){
	%>
		<div class="col-md-4 col-md-offset-4 col-sm-4 col-sm-offset-4 col-xs-5 col-xs-offset-4">
			<form action="login.jsp" method="GET">
				<div class="form-group">
					<input class="form-control" type="text" name="username" placeholder="Username">
				</div>
				<div class="form-group">
					<input class="form-control" type="password" name="password" placeholder="Password">
				</div  >
				<div class="centerButton">
					<input type="submit" class="btn btn-primary btn-lg" value="Login">
				</div>
			</form>
		</div>
		
	<%
			}else{
				//use attributes given to login to the database
				Player p = new Player(username, password);
				json.setParameters("player", gson.toJson(p, Player.class));
				String jsonResponse = json.getJson();
				
				//based on the json response, either result is null(bad url), or if fine, the player's details 
				Player result = (jsonResponse != null) ? (Player)gson.fromJson(jsonResponse, Player.class) : null;
				
				//based on the result:
				if(result != null){
					
					//save info into the session (after refresh it will forward to page)
					session.setAttribute("username", result.getUsername());
					session.setAttribute("password", result.getPassword());
					message = "Success!";
					response.sendRedirect("user.jsp");
				}else{
					
					//set initial "null" values and send a message to the user
					username = null;
					password = null;
					message = "Wrong details";
				}
	%>			
				<script>alert("<%= message %>")</script>
				<script>window.location = "http://localhost:8080/TTWS"</script>
	<%
			}
		}else{ 
		// forward to the next page
			response.sendRedirect("user.jsp");
		}
	%>
	</div>
</div>

<footer class="container-fluid footerSpace">
</footer>




</body>
</html>
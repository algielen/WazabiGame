package ovh.gorillahack.wazabi.api;

import java.io.IOException;

import javax.ejb.EJB;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.json.simple.JSONObject;

import ovh.gorillahack.wazabi.exception.NoCurrentGameException;
import ovh.gorillahack.wazabi.usecases.GestionPartie;

@WebServlet("/api/game/next")
public class GameNext extends HttpServlet {
	
	private static final long serialVersionUID = 1L;
	
    @EJB
    private GestionPartie gestionPartie;
	
	@SuppressWarnings("unchecked")
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		
		JSONObject jsonResponse = new JSONObject();
		
		try {
			
			gestionPartie.terminerTour();
			jsonResponse.put("status", true);
			
		} catch (NoCurrentGameException e) {
			jsonResponse.put("status", false);
		}

		response.getWriter().println(jsonResponse.toJSONString());
	}


}

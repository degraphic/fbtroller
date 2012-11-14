package com.mihaibojin.fbtroller;

import java.io.IOException;
import javax.servlet.http.*;

@SuppressWarnings("serial")
public class FacebookLoginServlet extends HttpServlet {
	public void doGet(HttpServletRequest req, HttpServletResponse resp)
			throws IOException {
		resp.setContentType("text/html");
		resp.getWriter().println("Login");
		
		resp.getWriter().println(req.getContextPath());
	}
}

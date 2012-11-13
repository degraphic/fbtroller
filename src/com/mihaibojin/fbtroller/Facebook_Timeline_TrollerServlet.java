package com.mihaibojin.fbtroller;

import java.io.IOException;
import javax.servlet.http.*;

@SuppressWarnings("serial")
public class Facebook_Timeline_TrollerServlet extends HttpServlet {
	public void doGet(HttpServletRequest req, HttpServletResponse resp)
			throws IOException {
		resp.setContentType("text/html");
		//resp.getWriter().println("Hello, world");
		
		resp.getWriter().println(req.getContextPath());
	}
}

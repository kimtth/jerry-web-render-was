//cd webctx\WEB-INF\classes
//javac -classpath ..\..\..\classes CalleeImpl.java //set a class path

package org.web.labs.inside.jerry.was.toyservlet;

public class ToyServlet implements IToy {
	private String name = "toyServlet";

	public void setName(String name) {
		this.name = name;
	}

	public String getName() {
		return name;
	}

	@Override
	public String doService() {
		name += "<html><h1>" + name + "<h1></html>";
		return name;
	}
}

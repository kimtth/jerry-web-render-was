package org.web.labs.inside.jerry.was;

import java.io.File;
import java.io.FileFilter;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.List;

import org.web.labs.inside.jerry.was.http.SimpleHttpServer;
import org.web.labs.inside.jerry.was.toyservlet.IToy;

public class SimpleContainer {
	public static void main(String[] args) throws ClassNotFoundException {
		System.out.println("start >>>");
        
		//http://qiita.com/opengl-8080/items/ca152658a0e52c786029
        SimpleHttpServer server = new SimpleHttpServer();
        SimpleContainer cl = new SimpleContainer();
        server.setContainer(cl);
        server.start();
	}

	public String action(String servletname) throws Exception {
		init();
		String msg = load(servletname);
		return msg;
	}

	private void init() {
		String contextPath = "D:\\was\\toyservlet";
		String classesPath = contextPath.concat(File.separator).concat("WEB-INF").concat(File.separator)
				.concat("classes");
		String libPath = contextPath.concat(File.separator).concat("WEB-INF").concat(File.separator).concat("lib");
		File classes = new File(classesPath);
		List<URL> urlList = new ArrayList<URL>();
		if (classes.exists()) {
			try {
				urlList.add(classes.toURI().toURL());
			} catch (MalformedURLException e) {
				e.printStackTrace();
			}
		}
		File lib = new File(libPath);
		if (lib.exists()) {
			try {
				FileFilter ff = new FileFilter() {
					@Override
					public boolean accept(File pathname) {
						boolean result = false;
						if (pathname.getName().endsWith(".jar")) {
							result = true;
						}
						return result;
					}
				};
				File[] jarList = lib.listFiles(ff);
				for (File file : jarList) {
					urlList.add(file.toURI().toURL());
				}
			} catch (MalformedURLException e) {
				e.printStackTrace();
			}
		}
		URL[] urls = new URL[urlList.size()];
		for (int i = 0; i < urls.length; i++) {
			urls[i] = urlList.get(i);
		}
		urlCL = new URLClassLoader(urls, Thread.currentThread().getContextClassLoader());
		Thread.currentThread().setContextClassLoader(urlCL);
	}

	private String load(String servletname) throws Exception {
		Class calleeClass = null;
		calleeClass = urlCL.loadClass("org.web.labs.inside.jerry.was.toyservlet." + servletname); //need to set fully qualified class name
		IToy calleeInstance = (IToy) calleeClass.newInstance();

		return calleeInstance.doService();
	}

	URLClassLoader urlCL = null;
}

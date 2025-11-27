package org.web.labs.inside.jerry.was;

import java.io.Closeable;
import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.web.labs.inside.jerry.was.http.SimpleHttpServer;
import org.web.labs.inside.jerry.was.toyservlet.IToy;

/**
 * SimpleContainer - A lightweight servlet container implementation.
 * 
 * Features:
 * - Servlet registration and caching
 * - Configurable context path
 * - Proper resource cleanup
 * - Thread-safe servlet management
 */
public class SimpleContainer implements Closeable {
	
	private static final Logger LOGGER = Logger.getLogger(SimpleContainer.class.getName());
	
	// Configuration
	private String contextPath;
	private String basePackage;
	
	// Servlet management
	private final Map<String, IToy> servletCache = new ConcurrentHashMap<>();
	private URLClassLoader urlClassLoader;
	private boolean initialized = false;
	
	// Default configuration
	private static final String DEFAULT_CONTEXT_PATH = "." + File.separator + "webapps";
	private static final String DEFAULT_BASE_PACKAGE = "org.web.labs.inside.jerry.was.toyservlet.";
	private static final int DEFAULT_PORT = 8080;
	
	/**
	 * Create a container with default configuration.
	 */
	public SimpleContainer() {
		this(DEFAULT_CONTEXT_PATH, DEFAULT_BASE_PACKAGE);
	}
	
	/**
	 * Create a container with custom configuration.
	 */
	public SimpleContainer(String contextPath, String basePackage) {
		this.contextPath = contextPath;
		this.basePackage = basePackage;
	}
	
	public static void main(String[] args) {
		int port = DEFAULT_PORT;
		String contextPath = DEFAULT_CONTEXT_PATH;
		
		// Parse command line arguments
		for (int i = 0; i < args.length; i++) {
			switch (args[i]) {
				case "-p":
				case "--port":
					if (i + 1 < args.length) {
						port = Integer.parseInt(args[++i]);
					}
					break;
				case "-c":
				case "--context":
					if (i + 1 < args.length) {
						contextPath = args[++i];
					}
					break;
				case "-h":
				case "--help":
					printHelp();
					return;
			}
		}
		
		LOGGER.info("Starting SimpleContainer on port " + port);
		LOGGER.info("Context path: " + contextPath);
		
		try (SimpleContainer container = new SimpleContainer(contextPath, DEFAULT_BASE_PACKAGE)) {
			SimpleHttpServer server = new SimpleHttpServer(port);
			server.setContainer(container);
			
			// Add shutdown hook for graceful shutdown
			Runtime.getRuntime().addShutdownHook(new Thread(() -> {
				LOGGER.info("Shutting down SimpleContainer...");
				server.stop();
				try {
					container.close();
				} catch (IOException e) {
					LOGGER.log(Level.WARNING, "Error during shutdown", e);
				}
			}));
			
			server.start();
		} catch (Exception e) {
			LOGGER.log(Level.SEVERE, "Failed to start container", e);
			System.exit(1);
		}
	}
	
	private static void printHelp() {
		System.out.println("SimpleContainer - A lightweight servlet container");
		System.out.println();
		System.out.println("Usage: java SimpleContainer [options]");
		System.out.println();
		System.out.println("Options:");
		System.out.println("  -p, --port <port>      Server port (default: 8080)");
		System.out.println("  -c, --context <path>   Context path (default: ./webapps)");
		System.out.println("  -h, --help             Show this help message");
	}
	
	/**
	 * Execute a servlet action.
	 */
	public String action(String servletName) throws ServletException {
		try {
			ensureInitialized();
			IToy servlet = getOrLoadServlet(servletName);
			return servlet.doService();
		} catch (Exception e) {
			throw new ServletException("Failed to execute servlet: " + servletName, e);
		}
	}
	
	/**
	 * Get servlet from cache or load it.
	 */
	private IToy getOrLoadServlet(String servletName) throws Exception {
		return servletCache.computeIfAbsent(servletName, name -> {
			try {
				return loadServlet(name);
			} catch (Exception e) {
				throw new RuntimeException("Failed to load servlet: " + name, e);
			}
		});
	}
	
	/**
	 * Load a servlet class dynamically.
	 */
	private IToy loadServlet(String servletName) throws Exception {
		String className = basePackage + servletName;
		LOGGER.info("Loading servlet: " + className);
		
		Class<?> servletClass = urlClassLoader.loadClass(className);
		
		if (!IToy.class.isAssignableFrom(servletClass)) {
			throw new ServletException("Class " + className + " does not implement IToy interface");
		}
		
		IToy instance = (IToy) servletClass.newInstance();
		LOGGER.info("Servlet loaded successfully: " + servletName);
		return instance;
	}
	
	/**
	 * Initialize the container (lazy initialization).
	 */
	private synchronized void ensureInitialized() {
		if (initialized) {
			return;
		}
		
		LOGGER.info("Initializing container with context: " + contextPath);
		
		List<URL> urlList = new ArrayList<>();
		
		// Add classes directory
		File classesDir = new File(contextPath, "WEB-INF" + File.separator + "classes");
		addDirectoryToClasspath(classesDir, urlList);
		
		// Add lib JARs
		File libDir = new File(contextPath, "WEB-INF" + File.separator + "lib");
		addJarsToClasspath(libDir, urlList);
		
		// Create URLClassLoader
		URL[] urls = urlList.toArray(new URL[0]);
		urlClassLoader = new URLClassLoader(urls, Thread.currentThread().getContextClassLoader());
		Thread.currentThread().setContextClassLoader(urlClassLoader);
		
		initialized = true;
		LOGGER.info("Container initialized with " + urls.length + " classpath entries");
	}
	
	private void addDirectoryToClasspath(File directory, List<URL> urlList) {
		if (directory.exists() && directory.isDirectory()) {
			try {
				urlList.add(directory.toURI().toURL());
				LOGGER.fine("Added to classpath: " + directory.getAbsolutePath());
			} catch (MalformedURLException e) {
				LOGGER.log(Level.WARNING, "Failed to add directory to classpath: " + directory, e);
			}
		}
	}
	
	private void addJarsToClasspath(File libDir, List<URL> urlList) {
		if (!libDir.exists() || !libDir.isDirectory()) {
			return;
		}
		
		FileFilter jarFilter = file -> file.getName().toLowerCase().endsWith(".jar");
		File[] jarFiles = libDir.listFiles(jarFilter);
		
		if (jarFiles != null) {
			for (File jarFile : jarFiles) {
				try {
					urlList.add(jarFile.toURI().toURL());
					LOGGER.fine("Added JAR to classpath: " + jarFile.getName());
				} catch (MalformedURLException e) {
					LOGGER.log(Level.WARNING, "Failed to add JAR to classpath: " + jarFile, e);
				}
			}
		}
	}
	
	/**
	 * Register a servlet instance directly.
	 */
	public void registerServlet(String name, IToy servlet) {
		servletCache.put(name, servlet);
		LOGGER.info("Registered servlet: " + name);
	}
	
	/**
	 * Unregister a servlet.
	 */
	public void unregisterServlet(String name) {
		servletCache.remove(name);
		LOGGER.info("Unregistered servlet: " + name);
	}
	
	/**
	 * Get list of registered servlets.
	 */
	public List<String> getRegisteredServlets() {
		return new ArrayList<>(servletCache.keySet());
	}
	
	/**
	 * Clear servlet cache (force reload on next request).
	 */
	public void clearCache() {
		servletCache.clear();
		LOGGER.info("Servlet cache cleared");
	}
	
	@Override
	public void close() throws IOException {
		LOGGER.info("Closing container...");
		servletCache.clear();
		if (urlClassLoader != null) {
			urlClassLoader.close();
		}
		initialized = false;
		LOGGER.info("Container closed");
	}
	
	// Getters and setters
	public String getContextPath() {
		return contextPath;
	}
	
	public void setContextPath(String contextPath) {
		this.contextPath = contextPath;
	}
	
	public String getBasePackage() {
		return basePackage;
	}
	
	public void setBasePackage(String basePackage) {
		this.basePackage = basePackage;
	}
	
	/**
	 * Custom exception for servlet-related errors.
	 */
	public static class ServletException extends Exception {
		private static final long serialVersionUID = 1L;
		
		public ServletException(String message) {
			super(message);
		}
		
		public ServletException(String message, Throwable cause) {
			super(message, cause);
		}
	}
}

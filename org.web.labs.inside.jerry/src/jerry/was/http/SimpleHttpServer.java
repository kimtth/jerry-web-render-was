package org.web.labs.inside.jerry.was.http;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.web.labs.inside.jerry.was.SimpleContainer;
import org.web.labs.inside.jerry.was.status.ContentType;
import org.web.labs.inside.jerry.was.status.Status;

/**
 * SimpleHttpServer - A lightweight HTTP server implementation.
 * 
 * Features:
 * - Configurable port
 * - Thread pool for handling requests
 * - Servlet routing
 * - Static file serving
 * - Graceful shutdown
 */
public class SimpleHttpServer {

	private static final Logger LOGGER = Logger.getLogger(SimpleHttpServer.class.getName());
	
	private SimpleContainer container;
	private final ExecutorService threadPool;
	private final int port;
	private volatile boolean running = false;
	private ServerSocket serverSocket;
	
	// Configuration
	private static final int DEFAULT_PORT = 8080;
	private static final int THREAD_POOL_SIZE = 10;
	private static final int SHUTDOWN_TIMEOUT_SECONDS = 5;
	
	/**
	 * Create server with default port.
	 */
	public SimpleHttpServer() {
		this(DEFAULT_PORT);
	}
	
	/**
	 * Create server with custom port.
	 */
	public SimpleHttpServer(int port) {
		this.port = port;
		this.threadPool = Executors.newFixedThreadPool(THREAD_POOL_SIZE);
	}

	public void setContainer(SimpleContainer container) {
		this.container = container;
	}

	/**
	 * Start the server.
	 */
	public void start() {
		running = true;
		
		try {
			serverSocket = new ServerSocket(port);
			LOGGER.info("Server started on port " + port);
			LOGGER.info("Access at: http://localhost:" + port);
			
			while (running) {
				try {
					this.handleConnection(serverSocket);
				} catch (IOException e) {
					if (running) {
						LOGGER.log(Level.WARNING, "Error accepting connection", e);
					}
				}
			}
		} catch (IOException e) {
			LOGGER.log(Level.SEVERE, "Failed to start server on port " + port, e);
		}
	}
	
	/**
	 * Stop the server gracefully.
	 */
	public void stop() {
		LOGGER.info("Stopping server...");
		running = false;
		
		// Close server socket
		if (serverSocket != null && !serverSocket.isClosed()) {
			try {
				serverSocket.close();
			} catch (IOException e) {
				LOGGER.log(Level.WARNING, "Error closing server socket", e);
			}
		}
		
		// Shutdown thread pool
		threadPool.shutdown();
		try {
			if (!threadPool.awaitTermination(SHUTDOWN_TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
				threadPool.shutdownNow();
			}
		} catch (InterruptedException e) {
			threadPool.shutdownNow();
			Thread.currentThread().interrupt();
		}
		
		LOGGER.info("Server stopped");
	}

	private void handleConnection(ServerSocket server) throws IOException {
		Socket socket = server.accept();
		
		threadPool.execute(() -> {
			try (InputStream in = socket.getInputStream(); 
			     OutputStream out = socket.getOutputStream()) {

				HttpRequest request = new HttpRequest(in);
				HttpHeader header = request.getHeader();
				
				String path = header.getPath();
				LOGGER.fine("Request: " + header.getMethod() + " " + path);

				if (path.startsWith("/servlet/")) {
					handleServletRequest(path, out);
				} else if (path.equals("/health")) {
					handleHealthCheck(out);
				} else if (path.equals("/servlets")) {
					handleServletList(out);
				} else {
					handleStaticRequest(header, out);
				}
				
			} catch (EmptyRequestException e) {
				// Ignore empty requests (e.g., browser prefetch)
			} catch (IOException e) {
				throw new UncheckedIOException(e);
			} finally {
				closeSocket(socket);
			}
		});
	}
	
	private void handleServletRequest(String path, OutputStream out) throws IOException {
		String servletName = path.replace("/servlet/", "");
		
		try {
			String result = container.action(servletName);
			respondWithMessage(result, out);
		} catch (SimpleContainer.ServletException e) {
			LOGGER.log(Level.WARNING, "Servlet error: " + servletName, e);
			respondWithError(Status.INTERNAL_ERROR, "Servlet Error: " + e.getMessage(), out);
		} catch (Exception e) {
			LOGGER.log(Level.SEVERE, "Unexpected error handling servlet: " + servletName, e);
			respondWithError(Status.INTERNAL_ERROR, "Internal Server Error", out);
		}
	}
	
	private void handleStaticRequest(HttpHeader header, OutputStream out) throws IOException {
		if (header.isGetMethod()) {
			File file = new File(".", header.getPath());

			if (file.exists() && file.isFile()) {
				respondWithFile(file, out);
			} else {
				respondNotFound(out);
			}
		} else {
			respondOk(out);
		}
	}
	
	private void handleHealthCheck(OutputStream out) throws IOException {
		HttpResponse response = new HttpResponse(Status.OK);
		response.addHeader("Content-Type", ContentType.APPLICATION_JSON);
		response.setBody("{\"status\":\"healthy\",\"port\":" + port + "}");
		response.writeTo(out);
	}
	
	private void handleServletList(OutputStream out) throws IOException {
		HttpResponse response = new HttpResponse(Status.OK);
		response.addHeader("Content-Type", ContentType.APPLICATION_JSON);
		
		StringBuilder json = new StringBuilder("{\"servlets\":[");
		boolean first = true;
		for (String name : container.getRegisteredServlets()) {
			if (!first) json.append(",");
			json.append("\"").append(name).append("\"");
			first = false;
		}
		json.append("]}");
		
		response.setBody(json.toString());
		response.writeTo(out);
	}
	
	private void closeSocket(Socket socket) {
		try {
			socket.close();
		} catch (IOException e) {
			LOGGER.log(Level.FINE, "Error closing socket", e);
		}
	}

	private void respondNotFound(OutputStream out) throws IOException {
		respondWithError(Status.NOT_FOUND, "404 Not Found", out);
	}
	
	private void respondWithError(Status status, String message, OutputStream out) throws IOException {
		HttpResponse response = new HttpResponse(status);
		response.addHeader("Content-Type", ContentType.TEXT_PLAIN);
		response.setBody(message);
		response.writeTo(out);
	}

	private void respondWithFile(File file, OutputStream out) throws IOException {
		HttpResponse response = new HttpResponse(Status.OK);
		response.setBody(file);
		response.writeTo(out);
	}

	private void respondOk(OutputStream out) throws IOException {
		HttpResponse response = new HttpResponse(Status.OK);
		response.writeTo(out);
	}
	
	private void respondWithMessage(String message, OutputStream out) throws IOException {
		HttpResponse response = new HttpResponse(Status.OK);
		response.addHeader("Content-Type", ContentType.TEXT_HTML);
		response.setBody(message);
		response.writeTo(out);
	}
	
	// Getters
	public int getPort() {
		return port;
	}
	
	public boolean isRunning() {
		return running;
	}
}

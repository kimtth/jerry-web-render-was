package org.web.labs.inside.jerry.nio;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * NIOHttpServer - A non-blocking HTTP server using Java NIO.
 * 
 * This server demonstrates the use of Selectors and Channels for 
 * handling multiple connections without threads per connection.
 * 
 * Features:
 * - Non-blocking I/O using Selector
 * - Single-threaded event loop
 * - Simple HTTP request/response handling
 * - Graceful shutdown support
 */
public class NIOHttpServer {
    
    private static final Logger LOGGER = Logger.getLogger(NIOHttpServer.class.getName());
    
    private static final int DEFAULT_PORT = 8888;
    private static final int BUFFER_SIZE = 4096;
    private static final long SELECT_TIMEOUT = 1000L;
    
    private final int port;
    private final AtomicBoolean running = new AtomicBoolean(false);
    private Selector selector;
    private ServerSocketChannel serverChannel;
    
    public NIOHttpServer() {
        this(DEFAULT_PORT);
    }
    
    public NIOHttpServer(int port) {
        this.port = port;
    }
    
    public static void main(String[] args) {
        int port = DEFAULT_PORT;
        
        // Parse command line arguments
        for (int i = 0; i < args.length; i++) {
            if (("-p".equals(args[i]) || "--port".equals(args[i])) && i + 1 < args.length) {
                port = Integer.parseInt(args[++i]);
            } else if ("-h".equals(args[i]) || "--help".equals(args[i])) {
                printHelp();
                return;
            }
        }
        
        NIOHttpServer server = new NIOHttpServer(port);
        
        // Add shutdown hook
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            LOGGER.info("Shutting down server...");
            server.stop();
        }));
        
        server.start();
    }
    
    private static void printHelp() {
        System.out.println("NIOHttpServer - Non-blocking HTTP Server");
        System.out.println();
        System.out.println("Usage: java NIOHttpServer [options]");
        System.out.println();
        System.out.println("Options:");
        System.out.println("  -p, --port <port>    Server port (default: 8888)");
        System.out.println("  -h, --help           Show this help message");
    }
    
    /**
     * Start the server.
     */
    public void start() {
        try {
            // Open selector
            selector = Selector.open();
            
            // Open server socket channel
            serverChannel = ServerSocketChannel.open();
            serverChannel.configureBlocking(false);
            serverChannel.socket().bind(new InetSocketAddress(port));
            
            // Register for accept events
            serverChannel.register(selector, SelectionKey.OP_ACCEPT);
            
            running.set(true);
            LOGGER.info("NIO HTTP Server started on port " + port);
            LOGGER.info("Test with: curl http://localhost:" + port + "/");
            LOGGER.info("Or use NIOHttpClient to connect");
            
            // Event loop
            while (running.get()) {
                // Wait for events
                int readyCount = selector.select(SELECT_TIMEOUT);
                
                if (readyCount == 0) {
                    continue;
                }
                
                // Process ready keys
                Set<SelectionKey> selectedKeys = selector.selectedKeys();
                Iterator<SelectionKey> keyIterator = selectedKeys.iterator();
                
                while (keyIterator.hasNext()) {
                    SelectionKey key = keyIterator.next();
                    keyIterator.remove();
                    
                    try {
                        if (!key.isValid()) {
                            continue;
                        }
                        
                        if (key.isAcceptable()) {
                            handleAccept(key);
                        } else if (key.isReadable()) {
                            handleRead(key);
                        } else if (key.isWritable()) {
                            handleWrite(key);
                        }
                    } catch (IOException e) {
                        LOGGER.log(Level.WARNING, "Error handling key", e);
                        closeChannel(key);
                    }
                }
            }
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Server error", e);
        } finally {
            cleanup();
        }
    }
    
    /**
     * Stop the server.
     */
    public void stop() {
        running.set(false);
        if (selector != null) {
            selector.wakeup();
        }
    }
    
    /**
     * Handle new connection accept.
     */
    private void handleAccept(SelectionKey key) throws IOException {
        ServerSocketChannel serverChannel = (ServerSocketChannel) key.channel();
        SocketChannel clientChannel = serverChannel.accept();
        
        if (clientChannel != null) {
            clientChannel.configureBlocking(false);
            
            // Attach a new connection context
            ConnectionContext context = new ConnectionContext();
            clientChannel.register(selector, SelectionKey.OP_READ, context);
            
            LOGGER.info("New connection from: " + clientChannel.getRemoteAddress());
        }
    }
    
    /**
     * Handle read event.
     */
    private void handleRead(SelectionKey key) throws IOException {
        SocketChannel clientChannel = (SocketChannel) key.channel();
        ConnectionContext context = (ConnectionContext) key.attachment();
        
        ByteBuffer buffer = ByteBuffer.allocate(BUFFER_SIZE);
        int bytesRead = clientChannel.read(buffer);
        
        if (bytesRead == -1) {
            // Connection closed
            closeChannel(key);
            return;
        }
        
        if (bytesRead > 0) {
            buffer.flip();
            byte[] data = new byte[buffer.remaining()];
            buffer.get(data);
            
            // Append to request
            context.appendRequest(new String(data));
            
            // Check if request is complete (simple check for double CRLF)
            if (context.isRequestComplete()) {
                // Parse and log request
                String request = context.getRequest();
                LOGGER.info("Received request:\n" + request.substring(0, Math.min(200, request.length())));
                
                // Prepare response
                String response = buildResponse(request);
                context.setResponse(response);
                
                // Switch to write mode
                key.interestOps(SelectionKey.OP_WRITE);
            }
        }
    }
    
    /**
     * Handle write event.
     */
    private void handleWrite(SelectionKey key) throws IOException {
        SocketChannel clientChannel = (SocketChannel) key.channel();
        ConnectionContext context = (ConnectionContext) key.attachment();
        
        ByteBuffer responseBuffer = context.getResponseBuffer();
        
        if (responseBuffer.hasRemaining()) {
            clientChannel.write(responseBuffer);
        }
        
        if (!responseBuffer.hasRemaining()) {
            // Response sent, close connection
            LOGGER.info("Response sent, closing connection");
            closeChannel(key);
        }
    }
    
    /**
     * Build HTTP response based on request.
     */
    private String buildResponse(String request) {
        // Parse request line
        String[] lines = request.split("\r\n");
        String requestLine = lines.length > 0 ? lines[0] : "";
        String[] parts = requestLine.split(" ");
        
        String method = parts.length > 0 ? parts[0] : "GET";
        String path = parts.length > 1 ? parts[1] : "/";
        
        // Build response body
        StringBuilder body = new StringBuilder();
        body.append("<!DOCTYPE html>\n");
        body.append("<html>\n");
        body.append("<head><title>NIO HTTP Server</title></head>\n");
        body.append("<body>\n");
        body.append("<h1>Hello from NIO HTTP Server!</h1>\n");
        body.append("<p>Server Time: ").append(java.time.LocalDateTime.now()).append("</p>\n");
        body.append("<h2>Request Info</h2>\n");
        body.append("<ul>\n");
        body.append("<li>Method: ").append(method).append("</li>\n");
        body.append("<li>Path: ").append(path).append("</li>\n");
        body.append("</ul>\n");
        body.append("<h2>Available Endpoints</h2>\n");
        body.append("<ul>\n");
        body.append("<li><a href=\"/\">/</a> - This page</li>\n");
        body.append("<li><a href=\"/health\">/health</a> - Health check (JSON)</li>\n");
        body.append("<li><a href=\"/echo\">/echo</a> - Echo request</li>\n");
        body.append("</ul>\n");
        body.append("</body>\n");
        body.append("</html>\n");
        
        String bodyStr = body.toString();
        String contentType = "text/html";
        
        // Handle different paths
        if (path.equals("/health")) {
            bodyStr = "{\"status\":\"healthy\",\"server\":\"NIOHttpServer\",\"port\":" + port + "}";
            contentType = "application/json";
        } else if (path.equals("/echo")) {
            bodyStr = request;
            contentType = "text/plain";
        }
        
        // Build response
        StringBuilder response = new StringBuilder();
        response.append("HTTP/1.1 200 OK\r\n");
        response.append("Content-Type: ").append(contentType).append("\r\n");
        response.append("Content-Length: ").append(bodyStr.getBytes().length).append("\r\n");
        response.append("Connection: close\r\n");
        response.append("Server: NIOHttpServer/1.0\r\n");
        response.append("\r\n");
        response.append(bodyStr);
        
        return response.toString();
    }
    
    /**
     * Close a channel associated with a key.
     */
    private void closeChannel(SelectionKey key) {
        try {
            key.cancel();
            key.channel().close();
        } catch (IOException e) {
            LOGGER.log(Level.FINE, "Error closing channel", e);
        }
    }
    
    /**
     * Cleanup resources.
     */
    private void cleanup() {
        try {
            if (serverChannel != null && serverChannel.isOpen()) {
                serverChannel.close();
            }
            if (selector != null && selector.isOpen()) {
                selector.close();
            }
        } catch (IOException e) {
            LOGGER.log(Level.WARNING, "Error during cleanup", e);
        }
        LOGGER.info("Server stopped");
    }
    
    /**
     * Connection context to track request/response state.
     */
    private static class ConnectionContext {
        private final StringBuilder requestBuilder = new StringBuilder();
        private ByteBuffer responseBuffer;
        
        public void appendRequest(String data) {
            requestBuilder.append(data);
        }
        
        public String getRequest() {
            return requestBuilder.toString();
        }
        
        public boolean isRequestComplete() {
            // Simple check: request ends with double CRLF
            String request = requestBuilder.toString();
            return request.contains("\r\n\r\n") || request.contains("\n\n");
        }
        
        public void setResponse(String response) {
            responseBuffer = ByteBuffer.wrap(response.getBytes());
        }
        
        public ByteBuffer getResponseBuffer() {
            return responseBuffer;
        }
    }
}

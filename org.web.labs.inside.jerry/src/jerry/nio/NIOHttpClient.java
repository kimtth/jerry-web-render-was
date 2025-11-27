package org.web.labs.inside.jerry.nio;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * NIOHttpClient - A non-blocking HTTP client using Java NIO.
 * 
 * This client demonstrates the use of Selectors and Channels for 
 * handling HTTP requests in a non-blocking manner.
 * 
 * Features:
 * - Non-blocking I/O using Selector
 * - HTTP response parsing with state machine
 * - Support for Content-Length and chunked transfer encoding
 * - Command-line interface for testing
 * 
 * Usage:
 *   java NIOHttpClient                          # Default: GET http://localhost:8888/
 *   java NIOHttpClient -h host -p port -P path  # Custom request
 */
public class NIOHttpClient {
    
    public static void main(String[] args) {
        // Default values
        String host = "localhost";
        int port = 8888;
        String path = "/";
        
        // Parse command line arguments
        for (int i = 0; i < args.length; i++) {
            if (("-h".equals(args[i]) || "--host".equals(args[i])) && i + 1 < args.length) {
                host = args[++i];
            } else if (("-p".equals(args[i]) || "--port".equals(args[i])) && i + 1 < args.length) {
                port = Integer.parseInt(args[++i]);
            } else if (("-P".equals(args[i]) || "--path".equals(args[i])) && i + 1 < args.length) {
                path = args[++i];
            } else if ("--help".equals(args[i])) {
                printHelp();
                return;
            }
        }
        
        // Build HTTP request
        String requestLine = "GET " + path + " HTTP/1.1";
        Map<String, String> headerMap = new HashMap<>();
        headerMap.put("Host", host + ":" + port);
        headerMap.put("User-Agent", "NIOHttpClient/1.0");
        
        // Create client and send request
        NIOHttpClient client = new NIOHttpClient(host, port, requestLine, headerMap, null);
        client.send();
    }
    
    private static void printHelp() {
        System.out.println("NIOHttpClient - Non-blocking HTTP Client");
        System.out.println();
        System.out.println("Usage: java NIOHttpClient [options]");
        System.out.println();
        System.out.println("Options:");
        System.out.println("  -h, --host <host>    Target host (default: localhost)");
        System.out.println("  -p, --port <port>    Target port (default: 8888)");
        System.out.println("  -P, --path <path>    Request path (default: /)");
        System.out.println("      --help           Show this help message");
        System.out.println();
        System.out.println("Examples:");
        System.out.println("  java NIOHttpClient                        # Connect to localhost:8888/");
        System.out.println("  java NIOHttpClient -h example.com -p 80   # Connect to example.com:80/");
    }

    private String host;
    private int port;
    private byte[] sendBytes;

    public NIOHttpClient(String host, int port, String requestLine, Map<String, String> headerMap, byte[] bodyBytes) {
        this.host = host;
        this.port = port;
        // Build HTTP request from components
        StringBuilder sb = new StringBuilder();
        sb.append(requestLine).append("\r\n");
        Set<String> headerKeySet = headerMap.keySet();
        Iterator<String> headerKeyIter = headerKeySet.iterator();
        while (headerKeyIter.hasNext()) {
            String name = headerKeyIter.next();
            String value = headerMap.get(name);
            sb.append(name).append(": ").append(value).append("\r\n");
        }
        // Add Content-Length if body exists
        if (bodyBytes != null)
            sb.append("Content-Length: ").append(bodyBytes.length).append("\r\n");
        // Request connection close
        sb.append("Connection: close\r\n");
        // End of headers
        sb.append("\r\n");
        byte[] headerBytes = sb.toString().getBytes();
        // Combine header and body
        if (bodyBytes == null) {
            sendBytes = headerBytes;
        } else {
            sendBytes = new byte[headerBytes.length + bodyBytes.length];
            System.arraycopy(headerBytes, 0, sendBytes, 0, headerBytes.length);
            System.arraycopy(bodyBytes, 0, sendBytes, headerBytes.length, bodyBytes.length);
        }
    }

    public static final long SELECT_INTERVAL = 200L;
    public static final int BUFFER_SIZE = 256;
    private static final ByteBuffer readBuffer = ByteBuffer.allocate(NIOHttpClient.BUFFER_SIZE);

    /**
     * Send the HTTP request and receive the response.
     */
    public void send() {
        SocketChannel socketChannel = null;
        Selector selector = null;
        boolean isEnd = false;
        try {
            // Open socket channel in non-blocking mode
            socketChannel = SocketChannel.open(new InetSocketAddress(host, port));
            socketChannel.configureBlocking(false);
            // Send the request
            socketChannel.write(ByteBuffer.wrap(sendBytes));
            // Register for read events
            selector = Selector.open();
            socketChannel.register(selector, SelectionKey.OP_READ);
            while (!isEnd) {
                // Wait for events
                selector.select(NIOHttpClient.SELECT_INTERVAL);
                Set<SelectionKey> selectionKeys = selector.keys();
                Iterator<SelectionKey> selectionKeyIter = selectionKeys.iterator();
                while (selectionKeyIter.hasNext()) {
                    SelectionKey selectionKey = selectionKeyIter.next();
                    if (selectionKey.isReadable()) {
                        // Get or create message bag
                        MessageBag messageBag = (MessageBag) selectionKey.attachment();
                        if (messageBag == null) {
                            messageBag = new MessageBag();
                            selectionKey.attach(messageBag);
                        }
                        // Read from channel
                        readBuffer.clear();
                        socketChannel.read(readBuffer);
                        readBuffer.flip();
                        while (readBuffer.hasRemaining()) {
                            byte oneByte = readBuffer.get();
                            // State machine for HTTP response parsing
                            if (messageBag.status == Status.INIT) {
                                messageBag.add(oneByte);
                                messageBag.status = Status.REQUEST_LINE;
                            } else if (messageBag.status == Status.REQUEST_LINE) {
                                if (oneByte == CR) {
                                    messageBag.status = Status.REQUEST_LINE_CR;
                                    messageBag.setRequestLine();
                                } else {
                                    messageBag.add(oneByte);
                                }
                            } else if (messageBag.status == Status.REQUEST_LINE_CR) {
                                messageBag.status = Status.REQUEST_LINE_CRLF;
                            } else if (messageBag.status == Status.REQUEST_LINE_CRLF) {
                                messageBag.add(oneByte);
                                messageBag.status = Status.HEADER;
                            } else if (messageBag.status == Status.HEADER) {
                                if (oneByte == CR) {
                                    messageBag.addHeader();
                                    messageBag.status = Status.HEADER_CR;
                                } else {
                                    messageBag.add(oneByte);
                                }
                            } else if (messageBag.status == Status.HEADER_CR) {
                                if (oneByte == LF) {
                                    messageBag.status = Status.HEADER_CRLF;
                                } else {
                                    throw new IllegalStateException("LF must be followed.");
                                }
                            } else if (messageBag.status == Status.HEADER_CRLF) {
                                if (oneByte == CR) {
                                    messageBag.status = Status.HEADER_CRLFCR;
                                } else {
                                    messageBag.add(oneByte);
                                    messageBag.status = Status.HEADER;
                                }
                            } else if (messageBag.status == Status.HEADER_CRLFCR) {
                                if (oneByte == LF) {
                                    BodyStyle bodyStyle = messageBag.afterHeader();
                                    if (bodyStyle == BodyStyle.NO_BODY) {
                                        messageBag.status = Status.TERMINATION;
                                        break;
                                    } else {
                                        messageBag.status = Status.BODY;
                                    }
                                } else {
                                    throw new IllegalStateException("LF must be followed.");
                                }
                            } else if (messageBag.status == Status.BODY) {
                                if (messageBag.bodyStyle == BodyStyle.CONTENT_LENGTH) {
                                    messageBag.add(oneByte);
                                    if (messageBag.getContentLength() <= messageBag.getBytesSize()) {
                                        messageBag.setBodyBytes();
                                        messageBag.status = Status.TERMINATION;
                                        break;
                                    }
                                } else if (messageBag.bodyStyle == BodyStyle.CHUNKED) {
                                    // Chunked transfer encoding
                                    if (messageBag.chunkStatus == ChunkStatus.CHUNK_NUM) {
                                        if (oneByte == CR) {
                                            messageBag.setChunkSize(
                                                    Integer.parseInt(new String(messageBag.toBytes()), 16));
                                            messageBag.chunkStatus = ChunkStatus.CHUNK_NUM_CR;
                                        } else {
                                            messageBag.add(oneByte);
                                        }
                                    } else if (messageBag.chunkStatus == ChunkStatus.CHUNK_NUM_CR) {
                                        if (oneByte == LF) {
                                            if (messageBag.getChunkSize() == 0) {
                                                messageBag.setChunkBodyBytes();
                                                messageBag.status = Status.TERMINATION;
                                                break;
                                            } else {
                                                messageBag.chunkStatus = ChunkStatus.CHUNK_BODY;
                                            }
                                        } else {
                                            throw new IllegalStateException("LF must be followed by CR");
                                        }
                                    } else if (messageBag.chunkStatus == ChunkStatus.CHUNK_BODY) {
                                        if (messageBag.getBytesSize() == messageBag.getChunkSize() - 1) {
                                            messageBag.add(oneByte);
                                            messageBag.addChunk();
                                            messageBag.chunkStatus = ChunkStatus.CHUNK_END;
                                        } else {
                                            messageBag.add(oneByte);
                                        }
                                    } else if (messageBag.chunkStatus == ChunkStatus.CHUNK_END) {
                                        if (oneByte == CR) {
                                            messageBag.chunkStatus = ChunkStatus.CHUNK_CR;
                                        } else {
                                            throw new IllegalStateException("CR must be followed by chunk");
                                        }
                                    } else if (messageBag.chunkStatus == ChunkStatus.CHUNK_CR) {
                                        if (oneByte == LF) {
                                            messageBag.chunkStatus = ChunkStatus.CHUNK_CRLF;
                                        } else {
                                            throw new IllegalStateException("LF must be followed by CR");
                                        }
                                    } else if (messageBag.chunkStatus == ChunkStatus.CHUNK_CRLF) {
                                        messageBag.add(oneByte);
                                        messageBag.chunkStatus = ChunkStatus.CHUNK_NUM;
                                    }
                                }
                            }
                        }
                        selectionKey.attach(messageBag);
                        if (messageBag.status == Status.TERMINATION) {
                            selectionKey.attach(null);
                            isEnd = true;
                            messageBag.process();
                            break;
                        }
                    }
                }
            }
            socketChannel.close();
            selector.close();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (socketChannel != null) {
                try {
                    socketChannel.close();
                } catch (IOException e) {
                }
            }
            if (selector != null) {
                try {
                    selector.close();
                } catch (IOException e) {
                }
            }
        }
    }

    public static final byte CR = '\r';
    public static final byte LF = '\n';

    public enum Status {
        INIT, REQUEST_LINE, REQUEST_LINE_CR, REQUEST_LINE_CRLF, HEADER, HEADER_CR, HEADER_CRLF, HEADER_CRLFCR, BODY, TERMINATION
    }

    public enum BodyStyle {
        NO_BODY, CONTENT_LENGTH, CHUNKED
    }

    public enum ChunkStatus {
        CHUNK_NUM, CHUNK_NUM_CR, CHUNK_NUM_CRLF, CHUNK_BODY, CHUNK_END, CHUNK_CR, CHUNK_CRLF
    }

    class MessageBag {
        private Status status = Status.INIT;
        private List<Byte> byteList = new ArrayList<Byte>();

        protected byte[] toBytes() {
            byte[] bytes = new byte[byteList.size()];
            for (int i = 0; i < byteList.size(); i++) {
                bytes[i] = byteList.get(i);
            }
            byteList.clear();
            return bytes;
        }

        protected void add(byte oneByte) {
            byteList.add(oneByte);
        }

        protected int getBytesSize() {
            return byteList.size();
        }

        private String requestLine;

        protected String getRequestLine() {
            return requestLine;
        }

        protected void setRequestLine() {
            requestLine = new String(toBytes());
        }

        private Map<String, String> headerMap = new HashMap<String, String>();

        protected void addHeader() {
            String headerLine = new String(toBytes());
            int indexOfColon = headerLine.indexOf(":");
            headerMap.put(headerLine.substring(0, indexOfColon).trim(), headerLine.substring(indexOfColon + 1).trim());
        }

        private int contentLength;

        protected int getContentLength() {
            return contentLength;
        }

        private BodyStyle bodyStyle;
        private ChunkStatus chunkStatus;
        private int chunkSize = -1;

        protected void setChunkSize(int chunkSize) {
            this.chunkSize = chunkSize;
        }

        protected int getChunkSize() {
            return chunkSize;
        }

        protected BodyStyle afterHeader() {
            bodyStyle = BodyStyle.NO_BODY;
            Set<String> headerKeySet = headerMap.keySet();
            Iterator<String> headerKeyIter = headerKeySet.iterator();
            while (headerKeyIter.hasNext()) {
                String headerName = headerKeyIter.next();
                String headerValue = headerMap.get(headerName);
                if ("Content-Length".equals(headerName)) {
                    contentLength = Integer.parseInt(headerValue);
                    bodyStyle = BodyStyle.CONTENT_LENGTH;
                } else if ("Transfer-Encoding".equals(headerName) && "chunked".equals(headerValue)) {
                    bodyStyle = BodyStyle.CHUNKED;
                    chunkStatus = ChunkStatus.CHUNK_NUM;
                }
            }
            return bodyStyle;
        }

        private byte[] bodyBytes;

        protected void setBodyBytes() {
            bodyBytes = toBytes();
        }

        protected byte[] getBodyBytes() {
            return bodyBytes;
        }

        private List<byte[]> chunkList = new ArrayList<byte[]>();

        protected void addChunk() {
            chunkList.add(toBytes());
        }

        protected void setChunkBodyBytes() {
            int bodyBytesLength = 0;
            for (int i = 0; i < chunkList.size(); i++) {
                bodyBytesLength = +chunkList.get(i).length;
            }
            bodyBytes = new byte[bodyBytesLength];
            int destPos = 0;
            for (int i = 0; i < chunkList.size(); i++) {
                System.arraycopy(chunkList.get(i), 0, bodyBytes, destPos, chunkList.get(i).length);
                destPos = +chunkList.get(i).length;
            }
        }

        protected void process() {
            System.out.println("=== HTTP Response ===");
            System.out.printf("Status Line: %s\n", requestLine);
            System.out.println("\n--- Headers ---");
            Set<String> headerKeySet = headerMap.keySet();
            Iterator<String> headerKeyIter = headerKeySet.iterator();
            while (headerKeyIter.hasNext()) {
                String headerName = headerKeyIter.next();
                String headerValue = headerMap.get(headerName);
                System.out.printf("%s: %s\n", headerName, headerValue);
            }
            System.out.println("\n--- Body ---");
            if (bodyBytes != null) {
                System.out.println(new String(getBodyBytes()));
            } else {
                System.out.println("(no body)");
            }
            System.out.println("=====================");
        }
    }
}

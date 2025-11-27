package org.web.labs.inside.jerry.was.http;

import java.io.IOException;
import java.io.InputStream;
import java.net.URLDecoder;
import java.util.HashMap;
import java.util.Map;

import org.web.labs.inside.jerry.was.status.HttpMethod;
import org.web.labs.inside.jerry.was.status.IOUtil;

import static org.web.labs.inside.jerry.was.status.Constant.*;

public class HttpHeader {
    
    private HttpMethod method;
    private String path;
    private String queryString;
    private final String headerText;
    private Map<String, String> messageHeaders = new HashMap<>();
    private Map<String, String> queryParams = new HashMap<>();
    
    public HttpHeader(InputStream in) throws IOException {
        StringBuilder header = new StringBuilder();

        header.append(this.readRequestLine(in))
              .append(this.readMessageLine(in));
        
        this.headerText = header.toString();
    }
    
    private String readRequestLine(InputStream in) throws IOException {
        String requestLine = IOUtil.readLine(in);
        
        String[] tmp = requestLine.split(" ");
        this.method = HttpMethod.valueOf(tmp[0].toUpperCase());
        
        String fullPath = URLDecoder.decode(tmp[1], "UTF-8");
        
        // Parse query string if present
        int queryIndex = fullPath.indexOf('?');
        if (queryIndex >= 0) {
            this.path = fullPath.substring(0, queryIndex);
            this.queryString = fullPath.substring(queryIndex + 1);
            parseQueryString();
        } else {
            this.path = fullPath;
            this.queryString = "";
        }
        
        return requestLine + CRLF;
    }
    
    private void parseQueryString() {
        if (queryString == null || queryString.isEmpty()) {
            return;
        }
        
        String[] pairs = queryString.split("&");
        for (String pair : pairs) {
            int eqIndex = pair.indexOf('=');
            if (eqIndex > 0) {
                String key = pair.substring(0, eqIndex);
                String value = eqIndex < pair.length() - 1 ? pair.substring(eqIndex + 1) : "";
                queryParams.put(key, value);
            }
        }
    }
    
    private StringBuilder readMessageLine(InputStream in) throws IOException {
        StringBuilder sb = new StringBuilder();
        
        String messageLine = IOUtil.readLine(in);
        
        while (messageLine != null && !messageLine.isEmpty()) {
            this.putMessageLine(messageLine);
            
            sb.append(messageLine + CRLF);
            messageLine = IOUtil.readLine(in);
        }
        
        return sb;
    }
    
    private void putMessageLine(String messageLine) {
        int colonIndex = messageLine.indexOf(':');
        if (colonIndex > 0) {
            String key = messageLine.substring(0, colonIndex).trim();
            String value = messageLine.substring(colonIndex + 1).trim();
            this.messageHeaders.put(key, value);
        }
    }

    public String getText() {
        return this.headerText;
    }
    
    public int getContentLength() {
        return Integer.parseInt(this.messageHeaders.getOrDefault("Content-Length", "0"));
    }

    public boolean isChunkedTransfer() {
        return this.messageHeaders.getOrDefault("Transfer-Encoding", "-").equals("chunked");
    }

    public String getPath() {
        return this.path;
    }
    
    public String getQueryString() {
        return this.queryString;
    }
    
    public String getQueryParam(String name) {
        return this.queryParams.get(name);
    }
    
    public Map<String, String> getQueryParams() {
        return new HashMap<>(this.queryParams);
    }

    public boolean isGetMethod() {
        return this.method == HttpMethod.GET;
    }
    
    public boolean isPostMethod() {
        return this.method == HttpMethod.POST;
    }
    
    public HttpMethod getMethod() {
        return this.method;
    }
    
    public String getHeader(String name) {
        return this.messageHeaders.get(name);
    }
}

package org.web.labs.inside.jerry.was.http;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;

import org.web.labs.inside.jerry.was.status.IOUtil;

public class HttpRequest {
    
    private final HttpHeader header;
    private final String bodyText;
    
    public HttpRequest(InputStream input) {
        try {
            this.header = new HttpHeader(input);
            this.bodyText = this.readBody(input);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
    
    private String readBody(InputStream in) throws IOException {
        if (this.header.isChunkedTransfer()) {
            return this.readBodyByChunkedTransfer(in);
        } else {
            return this.readBodyByContentLength(in);
        }
    }
    
    private String readBodyByChunkedTransfer(InputStream in) throws IOException {
        StringBuilder body = new StringBuilder();
        
        int chunkSize = Integer.parseInt(IOUtil.readLine(in), 16);
        
        while (chunkSize != 0) {
            byte[] buffer = new byte[chunkSize];
            in.read(buffer);
            
            body.append(IOUtil.toString(buffer));
            
            IOUtil.readLine(in); // chunk-body �겗�쑌弱얇겓�걗�굥 CRLF �굮沃��겳繇쎼겙�걲
            chunkSize = Integer.parseInt(org.web.labs.inside.jerry.was.status.IOUtil.readLine(in), 16);
        }
        
        return body.toString();
    }
    
    private String readBodyByContentLength(InputStream in) throws IOException {
        final int contentLength = this.header.getContentLength();
        
        if (contentLength <= 0) {
            return null;
        }
        
        byte[] buffer = new byte[contentLength];
        in.read(buffer);
        
        return IOUtil.toString(buffer);
    }
    
    public String getHeaderText() {
        return this.header.getText();
    }

    public String getBodyText() {
        return this.bodyText;
    }

    public HttpHeader getHeader() {
        return this.header;
    }
    
}

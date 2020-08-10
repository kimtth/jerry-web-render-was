package org.web.labs.inside.jerry.was.nio;

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

public class NIOHttpClient {
	public static void main(String[] args) {
		// HTTP ��û�� ������ ȣ��Ʈ ��� IP �� �����մϴ�.
		String host = "endofhope.com";
		int port = 80;
		// HTTP ��û�� ���� ���� �����մϴ�.
		// GET ������� / �� HTTP 1.1 ������ ����Ͽ� ��û�մϴ�.
		String requestLine = "GET / HTTP/1.1";
		// HTTP ����� �����մϴ�.
		// ���� Host ����� �ռ� �Է� ���� ȣ��Ʈ��:��Ʈ �� �����մϴ�.
		Map<String, String> headerMap = new HashMap<String, String>();
		headerMap.put("Host", host + ":" + port);
		// �̹� �������� GET ��� ȣ���̹Ƿ� HTTP �ٵ�� �����ϴ�.
		byte[] bodyBytes = null;
		// ���ݰ��� ������ ������ �����ڿ� �߰��Ͽ� ��ü�� �����ϰ� send �޽�带 ȣ���մϴ�.
		NIOHttpClient hc = new NIOHttpClient(host, port, requestLine, headerMap, bodyBytes);
		hc.send();
	}

	private String host;
	private int port;
	private byte[] sendBytes;

	public NIOHttpClient(String host, int port, String requestLine, Map<String, String> headerMap, byte[] bodyBytes) {
		this.host = host;
		this.port = port;
		// �����ڿ����� ���޹��� ���ڸ� �����Ͽ� HTTP ��û�� �����մϴ�.
		// request-line �� ����� \r\n �� ����Ͽ� �����մϴ�.
		StringBuilder sb = new StringBuilder();
		sb.append(requestLine).append("\r\n");
		Set<String> headerKeySet = headerMap.keySet();
		Iterator<String> headerKeyIter = headerKeySet.iterator();
		while (headerKeyIter.hasNext()) {
			String name = headerKeyIter.next();
			String value = headerMap.get(name);
			sb.append(name).append(": ").append(value).append("\r\n");
		}
		// ������ �ٵ� �ִٸ� Content-Length ����� �ٵ��� ũ�⸦ ������ �߰��մϴ�.
		if (bodyBytes != null)
			sb.append("Content-Length: ").append(bodyBytes.length).append("\r\n");
		// ������ ���� ���� ���� ���� Connection ��� ���� close �� �����Ͽ� �˷� �ݴϴ�.
		// ���� ������ �����̶�� ������ ��û�� ���Ƿ� ���� �� ���� ��û�ϴ� �ǹ̷� close ��� keep-alive ��
		// �����մϴ�.
		sb.append("Connection: close\r\n");
		// ������� ���� �ǹ��ϱ� ���� �� ���� (\r\n\r\n)�� �����ϴ�.
		sb.append("\r\n");
		byte[] headerBytes = sb.toString().getBytes();
		// ������ ������ �ǹ��ϴ� sendBytes �� ���ݱ��� ������ ������ �����մϴ�.
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

	// �����ڿ��� ���õ� ���� ������� ���� ������ �õ��մϴ�.
	public void send() {
		SocketChannel socketChannel = null;
		Selector selector = null;
		boolean isEnd = false;
		try {
			// ���� ���� ä���� ������ ��, �� ���� �� non-blocking ������ �߰��մϴ�.
			socketChannel = SocketChannel.open(new InetSocketAddress(host, port));
			socketChannel.configureBlocking(false);
			// �ռ� ���� ������ ������ sendBytes �� ByteBuffer ���·� �ٲ� �� �����մϴ�.
			socketChannel.write(ByteBuffer.wrap(sendBytes));
			// ���� ���������� �� ���������� �бⰡ ���۵˴ϴ�.
			// �����͸� �ϳ� ����, �б� �̺�Ʈ�� �߻��ϸ� ����ڴٴ� ���� �����մϴ�.
			selector = Selector.open();
			socketChannel.register(selector, SelectionKey.OP_READ);
			while (!isEnd) {
				// ���� SELECT_INTERVAL �ð� ��ŭ ������ ������ (READ) �̺�Ʈ�� ��ٸ��ϴ�.
				selector.select(NIOHttpClient.SELECT_INTERVAL);
				// �� ���� ��� select �޽�尡 ��ȯ�˴ϴ�.
				// ù ��°�� ����ϰ� �ִ� �̺�Ʈ�� �Դٸ� ��ȯ�˴ϴ�.
				// �� ��°�� �̺�Ʈ�� ���� �ʾƵ� ������ Ÿ�Ӿƿ� �ð�(SELECT_INTERVAL) �� ������ ��ȯ�˴ϴ�.
				// �� ��° ��쿡�� selectionKeys.iterator().hasNext() �� false �̹Ƿ� �ٽ�
				// ���� while ������ ���ư�
				// �ٽ� select �޽�忡�� �̺�Ʈ�� ����ϰ� �ǰڽ��ϴ�.
				Set<SelectionKey> selectionKeys = selector.keys();
				Iterator<SelectionKey> selectionKeyIter = selectionKeys.iterator();
				while (selectionKeyIter.hasNext()) {
					SelectionKey selectionKey = selectionKeyIter.next();
					// ���� ������ �̺�Ʈ�� ���Դٸ� SelectionKey �� ��ȯ�ǰ� �ǰ�
					// ���� �̺�Ʈ Ÿ�� ���� ������ ó���� ���۵˴ϴ�.
					// ������ READ �̺�Ʈ�� ���ؼ��� Selector �� ����Ͽ����Ƿ�
					// �̺�Ʈ�� ���Դٸ� �Ʒ� if ������ true �� �˴ϴ�.
					if (selectionKey.isReadable()) {
						// ���� �б� �̺�Ʈ�� �߻��Ͽ����� ���� ä�ο��� ���������� �а� �˴ϴ�.
						// ������ �о� �鿴�� �� �̹��� ���� ���� ��ü �޽������ ������ �����ϴ�.
						// �̷������� IP ��Ŷ�� �ִ� ũ��� 65535 �Դϴٸ� ���������δ� �� ���� �ξ� ���� ũ�Ⱑ
						// ���˴ϴ�.
						// ������ �� ������ �ѹ��� �о� ���� �� �ִ� �޽����� ũ�Ⱑ �ٸ� �� �����Ƿ�
						// TCP/IP�� �� ���� ����� ������ �� �ڽ��� MSS (Maximum Segment
						// Size)�� ��뿡�� �˷� �ְ� �˴ϴ�.
						// �� ������ ������� ���� �� �޽����� ����� MSS �� ���� ���� ������ ������ ������ �˴ϴ�.
						// ���� �� �� ���� ä�ο��� �о� ����, ���� �޽��� ������ �ִٸ�, �����Ͽ� �ΰ�
						// ���� �б� �̺�Ʈ�� �߻��Ͽ��� �� �ٽ� �б⸦ �õ��ϰ� �˴ϴ�.
						// �̸� ���� �����Ͽ� �δ� ���� �ٷ� SelectionKey.attachment �� �ǰڽ��ϴ�.
						MessageBag messageBag = (MessageBag) selectionKey.attachment();
						if (messageBag == null) {
							messageBag = new MessageBag();
							selectionKey.attach(messageBag);
						}
						// ���� �б⸦ �õ��մϴ�.
						// �б� ���۸� �ʱ�ȭ �ϰ� �н��ϴ�.
						readBuffer.clear();
						socketChannel.read(readBuffer);
						readBuffer.flip();
						while (readBuffer.hasRemaining()) {
							// �о���� ������ �տ��� ���� �� ����Ʈ �� ����
							// HTTP �޽������� ���� ��� �������� Ȯ���ϸ� ó���ϰ� �˴ϴ�.
							byte oneByte = readBuffer.get();
							// ó�� �����Դϴ�. ���¸� ��û �������� �ٲߴϴ�.
							if (messageBag.status == Status.INIT) {
								messageBag.add(oneByte);
								messageBag.status = Status.REQUEST_LINE;
								// ù ���� ��û ������ �о� ���̴ٰ� \r �� ������ �� ���� ������,
								// �� ��û ������ ����Ǿ����� �˼� �ֽ��ϴ�.
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
								// ��û ������ ����� ���Ŀ��� ����� �����մϴ�.
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
								// ��� ���¿��� \r �� ������ �ϳ��� ������� �ϼ��Ǿ��ٰ� �����մϴ�.
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
								// �� ������� ������ ���� ����ΰ� ������
								// ���ݱ��� ���� ��û ���ΰ� ����� �Ľ��Ͽ�
								// �޽��� �ٵ� ������ �Ǵ��Ͽ� �� �ʿ��ϸ� �б⸦ ����մϴ�.
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
								// �޽��� �ٵ� �ִ� ���
								if (messageBag.bodyStyle == BodyStyle.CONTENT_LENGTH) {
									// Content-Length ����� �ִٸ� �ش� ��� �� ��ŭ �ٵ� �߰���
									// �н��ϴ�.
									messageBag.add(oneByte);
									if (messageBag.getContentLength() <= messageBag.getBytesSize()) {
										// �޽��� �ٵ� ������ �� �о��ٸ�
										// ���ݱ��� ���� ���� bodyBytes �� �ְ�, ���¸� �����
										// ǥ���ϰ� �����ϴ�.
										messageBag.setBodyBytes();
										messageBag.status = Status.TERMINATION;
										break;
									}
								} else if (messageBag.bodyStyle == BodyStyle.CHUNKED) {
									// �޽��� ûũ ����� ��� HTTP �ٵ� ���Ͽ� �ٽ� ���� ��踦
									// ����ϴ�.
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
												// ũ�Ⱑ 0�� ûũ�� ûũ ��� �޽��� �ٵ��� ����
												// �ǹ��մϴ�.
												// �޽��� ���¸� ����� ǥ���ϰ� �����ϴ�.
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
						// �б� ���۸� ������ �� �о����ϴ�.
						// ���ݱ��� �о���� ���� �ٽ� SelectionKey.attachment �� �־� �Ӵϴ�.
						selectionKey.attach(messageBag);
						if (messageBag.status == Status.TERMINATION) {
							// �޽����� ������ �� ���� ���̶��
							// SelectionKey.attachment �� �����ϰ�
							// ���ݱ��� �о� ���� ���� ǥ���մϴ�.
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
			;
			if (selector != null) {
				try {
					selector.close();
				} catch (IOException e) {
				}
			}
			;
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
		// �� Ŭ������ HTTP �޽����� �����ϴ� ���� ��Ҹ� ǥ���� �� �ֵ��� �����Ǿ����ϴ�.
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

		// ����� �� ���� ���� Ȯ�εǸ�
		// ������ �̸�/�� ������ ����� �� �����ϰ�
		// �޽��� �ٵ� �ִ���,
		// �󸶳� Ȥ�� ��� �޽��� �ٵ� �о�� �ϴ��� Ȯ���ϴ� ������ �ϴ� �޽���Դϴ�.
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

		// ûũ ��� �ٵ��� ��� ���� ûũ���� �ϳ��� �ٵ� �̷�Ƿ� ����Ʈ ���·� �����մϴ�.
		private List<byte[]> chunkList = new ArrayList<byte[]>();

		protected void addChunk() {
			chunkList.add(toBytes());
		}

		// ���� ûũ �ٵ� �ϳ��� �ٵ�� �����ִ� ��ƿ��Ƽ �޽���Դϴ�.
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
			// �о���� HTTP �޽����� �����ֱ� ���� �޽���Դϴ�.
			System.out.printf("%s\n", requestLine);
			Set<String> headerKeySet = headerMap.keySet();
			Iterator<String> headerKeyIter = headerKeySet.iterator();
			while (headerKeyIter.hasNext()) {
				String headerName = headerKeyIter.next();
				String headerValue = headerMap.get(headerName);
				System.out.printf("%s: %s\n", headerName, headerValue);
			}
			System.out.printf("\n");
			if (bodyBytes != null) {
				System.out.println(new String(getBodyBytes()));
			}
		}
	}
}
//package package1;

// Created by Kevin Short on 10/12/2017
import java.io.*;
import java.net.*;
import java.util.*;

public class WebServer {

	public static void main(String args[]) throws Exception {
		int portNum = 1234;
		ServerSocket rendezvousSocket = new ServerSocket(portNum);
		while (true) {
			// listen for connection
			Socket connectionWClient = rendezvousSocket.accept();

			// Construct an object to process the HTTP request message.
			HttpRequest request = new HttpRequest(connectionWClient);
			// Create a new thread to process the request.
			Thread thread = new Thread(request);
			// Start the thread.
			thread.start();
		}
	}
}

class HttpRequest implements Runnable {
	static String CRLF = "\r\n";
	Socket mySocket;

	public HttpRequest(Socket clientSocket) throws Exception {
		this.mySocket = clientSocket;
	}

	public void run() {
		try {
			processRequest();
		} catch (Exception e) {
			System.out.println(e);
		}
	}

	private static String contentType(String fileName) {
		if (fileName.endsWith(".htm") || fileName.endsWith(".html")) {
			return "text/html";
		}
		if (fileName.endsWith(".gif")) {
			return "image/gif";
		}
		if (fileName.endsWith(".jpg")) {
			return "image/jpeg";
		}
		return "application/octet-stream";
	}

	private static void sendBytes(FileInputStream fis, DataOutputStream dos) throws Exception {
		// Construct a 1K buffer to hold bytes on their way to the socket.
		byte[] buffer = new byte[1024];
		int bytes = 0;

		// Copy requested line into the socket's output stream
		while ((bytes = fis.read(buffer)) != -1) {
			dos.write(buffer, 0, bytes);
		}
	}

	private void processRequest() throws Exception {

		// create streams to write/receive from socket
		InputStream is = mySocket.getInputStream();
		InputStreamReader isw = new InputStreamReader(is);
		BufferedReader fromClient = new BufferedReader(isw);
		OutputStream os = mySocket.getOutputStream();
		DataOutputStream dos = new DataOutputStream(os);
		OutputStreamWriter osw = new OutputStreamWriter(dos);
		BufferedWriter toClient = new BufferedWriter(osw);

		String requestLine = fromClient.readLine();
		// Display the request line.
		System.out.println();
		System.out.println(requestLine);

		// Get and display the header lines.
		String headerLine = null;
		while ((headerLine = fromClient.readLine()).length() != 0) {
			System.out.println(headerLine);
		}

		// Extract the filename from the request line.
		StringTokenizer tokens = new StringTokenizer(requestLine);
		tokens.nextToken(); // skip over the method, which should be "GET"
		String fileName = tokens.nextToken();
		// Prepend a "." so that file request is within the current directory.
		fileName = "." + fileName;

		// open the request file
		FileInputStream fis = null;
		boolean fileExists = true;
		try {
			fis = new FileInputStream(fileName);
		} catch (FileNotFoundException e) {
			fileExists = false;
		}

		// Construct the response message.
		String statusLine = null;
		String contentTypeLine = null;
		String entityBody = null;
		if (fileExists) {
			statusLine = "HTTP/1.1 200";
			contentTypeLine = "Content-type: " + contentType(fileName) + CRLF;
		} else {
			statusLine = "HTTP/1.1 404";
			contentTypeLine = "Content-type: " + contentType(fileName) + CRLF; // this line needs to be fixed
			entityBody = "<HTML>" + "<HEAD><TITLE>Not Found</TITLE></HEAD>" + "<BODY>Not Found</BODY></HTML>";
		}

		// Send the status line
		dos.writeBytes(statusLine);

		// Send the content type line.
		dos.writeBytes(contentTypeLine);

		// Send a blank line to indicate the end of the header lines
		dos.writeBytes(CRLF);

		// Send the entity body
		if (fileExists) {
			sendBytes(fis, dos);
			fis.close();
		} else {
			dos.writeBytes(entityBody);
		}

		// closing connections
		is.close();
		isw.close();
		fromClient.close();
		os.close();
		mySocket.close();
		osw.close();
		toClient.close();
	}
}

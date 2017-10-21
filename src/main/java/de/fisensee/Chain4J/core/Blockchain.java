package de.fisensee.Chain4J.core;

import java.io.IOException;
import java.io.InputStream;
import java.util.Scanner;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import fi.iki.elonen.NanoHTTPD;
import fi.iki.elonen.NanoHTTPD.Response.Status;

public class Blockchain extends NanoHTTPD {

	private static Gson gson = new Gson();
	private static JsonParser jsonParser = new JsonParser();
	
	private Node localNode;

	public Blockchain(int port) throws IOException {
		super(port);
		localNode = new Node();
		start(NanoHTTPD.SOCKET_READ_TIMEOUT, false);
		System.out.println("Blockchain running!");
	}
	
	public static void main(String[] args) {
		try {
			int port = Integer.valueOf(args[0]);
			new Blockchain(port);
		} catch (IOException e) {
			System.err.println("Could not start server:\n" + e);
		}
	}
	
	@Override
	public Response serve(IHTTPSession session) {
		String path = session.getUri();
		String message = "nothing";
		if(session.getMethod() == Method.GET) {
			switch (path) {
			case "/mine":
				return localNode.mine();
			case "/chain":
				return localNode.requestFullChain();
			case "/nodes/resolve":
				return localNode.consensus();
			default:
				break;
			}
		} else if(session.getMethod() == Method.POST) {
			InputStream input = session.getInputStream();
			Scanner s = new Scanner(input).useDelimiter("\\A");
			String rawBody = s.hasNext() ? s.next() : "";
			if(rawBody.equals(""))
				return NanoHTTPD.newFixedLengthResponse(Status.BAD_REQUEST, NanoHTTPD.MIME_PLAINTEXT, "missing values");
			JsonObject body = jsonParser.parse(rawBody).getAsJsonObject();
			switch (path) {
			case "/transactions/new":
				return localNode.requestNewTransaction(body);
			case "/nodes/register":
				return localNode.registerNodes(body);
			default:
				break;
			}
		}
		
		
		return newFixedLengthResponse(message);
	}

}

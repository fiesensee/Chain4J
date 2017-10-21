package de.fisensee.Chain4J.core;

import java.io.UnsupportedEncodingException;
import java.lang.reflect.Type;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.json.JSONArray;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.reflect.TypeToken;
import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.JsonNode;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;

import fi.iki.elonen.NanoHTTPD;
import fi.iki.elonen.NanoHTTPD.Response;
import fi.iki.elonen.NanoHTTPD.Response.Status;

public class Node {
	
	private List<Transaction> currentTransactions;
	private List<Block> chain;
	private Set<Node> nodes;
	
	private String host;
	private int port;
	private String identifier;
	
	private static Gson gson = new Gson();
	private static JsonParser jsonParser = new JsonParser();
	
	public Node() {
		currentTransactions = new ArrayList<>();
		chain = new ArrayList<>();
		nodes = new HashSet<>();
		
		identifier = UUID.randomUUID().toString().replace("-", "");
		
		Block genesisBlock = this.createNewBlock("1", 100);
		chain.add(genesisBlock);
	}
	
	private Node(String host, int port) {
		this.host = host;
		this.port = port;
	}
	
	private String getURL(String path) {
		return String.format("http://%s:%d%s", this.host, this.port, path);
	}

	/**
	 * Create a new Block on the blockchain
	 * 
	 * @param previousHash The hash of the previous block
	 * @param proof The proof given by the Proof of Work algo
	 */
	private Block createNewBlock(String previousHash, int proof) {
		if(previousHash == null || previousHash.equals("")) {
			previousHash = chain.get(chain.size() - 1).calculateHash();
		}
		
		Block block = new Block(chain.size() + 1, System.currentTimeMillis(), currentTransactions,
				proof, previousHash);

		currentTransactions = new ArrayList<>();
		
		return block;
	}

	/**
	 * Creates a new transaction to go into th next mined block
	 * @param sender Address of the sender
	 * @param recipient Address of the recipient
	 * @param amount Amount
	 * @return the index of the block that will hold this transaction
	 */
	private int createNewTransaction(String sender, String recipient, int amount) {
		Transaction transaction = new Transaction(sender, recipient, amount);
		
		currentTransactions.add(transaction);
		
		return chain.get(chain.size() - 1).getIndex() + 1;
	}

	/**
	 * Add a new node to the list of nodes
	 * @param nodeAddress The address of the new Node, for example http://192.168.0.5:5000
	 * @throws MalformedURLException 
	 */
	private void registerNode(String nodeAddress){
		try {
			URL nodeUrl = new URL(nodeAddress);
			Node node = new Node(nodeUrl.getHost(), nodeUrl.getPort());
			nodes.add(node);
		} catch (MalformedURLException e) {
//			should not happen, therefore ignore
			e.printStackTrace();
		}
	}
	
	/**
	 * Determine if a given chain is valid
	 * @param chain The chain which should be validated
	 * @return True if valid, false if not
	 */
	private boolean isValidChain(List<Block> chain) {
	
		Block lastBlock = chain.get(0);
		
		for(int currentIndex = 1; currentIndex  < chain.size(); currentIndex++) {
			Block block = chain.get(currentIndex);
		
//			check if the hash of the block is correct
			if(!block.getPreviousHash().equals(lastBlock.calculateHash())) {
				return false;
			}
		
//			check if the proof of work is correct
			if(!isValidProof(lastBlock.getProof(), block.getProof(), block.getPreviousHash())) {
				return false;
			}
			
			lastBlock = block;
		}
		
		return true;
	}
	
	/**
	 * This is our consensus algorithm, it resolves conflicts by replacing
	 * our chain with the longest one in the network
	 * @return True if our chain was replaced, false if not
	 */
	private boolean resolveConflicts() {
		
		Set<Node> neighbours = nodes;
		List<Block> newChain = new ArrayList<>();
		
		int maxLength = chain.size();
		
		for(Node node : neighbours) {
			try {
				HttpResponse<JsonNode> jsonResponse = Unirest.get(node.getURL("/chain")).asJson();
				JsonObject responseObject = jsonParser.parse(jsonResponse.getBody().toString()).getAsJsonObject();
				if(jsonResponse.getStatus() == 200) {
					int length = responseObject.get("length").getAsInt();
					JsonElement jsonChain = responseObject.get("chain");
					Type listType = new TypeToken<List<Block>>() {}.getType();
					List<Block> chain = gson.fromJson(jsonChain, listType);
//					check if length is longer and if the chain is valid
					if(length > maxLength && isValidChain(chain)) {
						maxLength = length;
						newChain = chain;
					}
				}
			} catch (UnirestException e) {
//				if exception continue with next node
				e.printStackTrace();
			}
		}
		
		if(!newChain.isEmpty()) {
			this.chain = newChain;
			return true;
		}

		return false;
	}
	
	private List<Block> parseJSONChain(JSONArray jsonChain) {
		return null;
	}
	
	
	/**
	 * Simple Proof of Work Algorithm:
	 * Find a number p' such that hash(pp'h) contains 4 leading zeroes, where p is the previous p'
	 * p is the previous proof, p' is the new proof and h is the hash of the last block
	 * @param lastBlock The last block
	 * @return True if valid, false if not
	 */
	private int proofOfWork(Block lastBlock) {

		
		int lastProof = lastBlock.getProof();
		String lastHash = lastBlock.getPreviousHash();
		int proof = 0;
		while (!isValidProof(lastProof, proof, lastHash)) {
			proof++;
		}
		
		return proof;
	}
	
	/**
	 * Validates the proof
	 * @param lastProof Previous proof
	 * @param newProof Current proof
	 * @param lastHash The hash of the previous block
	 * @return True if correct, false if not
	 */
	private boolean isValidProof(int lastProof, int newProof, String lastHash) {
		System.out.println(String.format("Cheking validity of proof (%s|%s|%s)", lastProof, newProof, lastHash));
		try {
			MessageDigest md = MessageDigest.getInstance("SHA-256");
			byte[] bytes = String.format("%d%d%s", lastProof, newProof, lastHash).getBytes("UTF-8");
			md.reset();
			md.update(bytes);
			byte[] digest = md.digest();
			String hash = Base64.getEncoder().encodeToString(digest); 
			System.out.println(String.format("calculated hash: %s", hash));
			for(int i = 0; i < 2; i++) {
				if(hash.charAt(i) != '0') {
					return false;
				}
			}
			return true;
		} catch (NoSuchAlgorithmException | UnsupportedEncodingException e) {
	//		should never happen
			e.printStackTrace();
			return false;
		}
	}
	
	public Response mine() {
//		we run the proof of work algorithm to get the next proof
		Block lastBlock = chain.get(chain.size() - 1);
		int proof = proofOfWork(lastBlock);
		
//		we must receive a reward for finding the proof
//		the sender is "0" to signify that this node has mined a new coin
		createNewTransaction("0", identifier, 1);
		
		Block newBlock = createNewBlock(lastBlock.calculateHash(), proof);
		chain.add(newBlock);
	
		JsonObject message = new JsonObject();
		message.addProperty("message", "New Block Forged");
		message.addProperty("index", newBlock.getIndex());
		message.addProperty("proof", proof);
		message.addProperty("previous_hash", newBlock.getPreviousHash());
		message.addProperty("hash", newBlock.calculateHash());
		JsonArray jsonTransactions = gson.toJsonTree(newBlock.getTransactions()).getAsJsonArray();
		message.add("transactions", jsonTransactions);
		
		return NanoHTTPD.newFixedLengthResponse(Status.OK, NanoHTTPD.MIME_PLAINTEXT, message.toString());
	}
	
	public Response requestNewTransaction(JsonObject jsonValues) {
		String sender = jsonValues.get("sender").getAsString();
		String recipient = jsonValues.get("recipient").getAsString();
		int amount = jsonValues.get("amount").getAsInt();
		
		if(sender == null || recipient == null || amount == 0) {
			return NanoHTTPD.newFixedLengthResponse(Status.BAD_REQUEST, NanoHTTPD.MIME_PLAINTEXT, "missing values");
		}
		
		int index = createNewTransaction(sender, recipient, amount);
		
		JsonObject message = new JsonObject();
		message.addProperty("message", String.format("Transaction will be added to Block %d", index));
		return NanoHTTPD.newFixedLengthResponse(Status.CREATED, NanoHTTPD.MIME_PLAINTEXT, message.toString());
	}
	
	public Response requestFullChain() {
		JsonObject message = new JsonObject();
		JsonArray jsonChain = gson.toJsonTree(chain).getAsJsonArray();
		message.add("chain", jsonChain);
		message.addProperty("length", chain.size());
	
		return NanoHTTPD.newFixedLengthResponse(Status.OK, NanoHTTPD.MIME_PLAINTEXT, message.toString());
	}
	
	public Response registerNodes(JsonObject jsonNodes) {
		JsonArray nodesArray = jsonNodes.getAsJsonArray("nodes");
		if(nodesArray == null) {
			return NanoHTTPD.newFixedLengthResponse(Status.BAD_REQUEST, NanoHTTPD.MIME_PLAINTEXT, "Error: Please supply a valid list of nodes");
		}

		for(JsonElement e : nodesArray) {
			String node = e.getAsString();
			registerNode(node);
		}
		
		JsonObject message = new JsonObject();
		message.addProperty("message", "New nodes have been added");
		JsonArray totalNodes = gson.toJsonTree(nodes).getAsJsonArray();
		message.add("total_nodes", totalNodes);
		
		return NanoHTTPD.newFixedLengthResponse(Status.CREATED, NanoHTTPD.MIME_PLAINTEXT, message.toString());
	}
	
	public Response consensus() {
		boolean isReplaced = resolveConflicts();
	
		JsonObject message = new JsonObject();
		JsonArray jsonChain = gson.toJsonTree(chain).getAsJsonArray();
		
		if(isReplaced) {
			message.addProperty("message", "Our chain was replaced");
			message.add("new_chain", jsonChain);
		} else {
			message.addProperty("message", "Our chain is authoritative");
			message.add("chain", jsonChain);
		}
		
		return NanoHTTPD.newFixedLengthResponse(Status.OK, NanoHTTPD.MIME_PLAINTEXT, message.toString());
	}

}

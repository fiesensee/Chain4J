package de.fisensee.Chain4J.core;

import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.List;

import com.google.gson.Gson;

import fi.iki.elonen.NanoHTTPD;

public class Block {
	
	private int index;
	private long timestamp;
	private List<Transaction> transactions;
	private int proof;
	private String previousHash;
	
	public Block(int index, long timestamp, List<Transaction> transactions, int proof, String previousHash) {
		super();
		this.index = index;
		this.timestamp = timestamp;
		this.transactions = transactions;
		this.proof = proof;
		this.previousHash = previousHash;
	}

	/**
	 * Creates a SHA-256 hash of this block
	 * @return the hash as a string
	 */
	public String calculateHash() {
		try {
			MessageDigest md = MessageDigest.getInstance("SHA-256");
			Gson gson = new Gson();
			String blockString = gson.toJson(this);
			byte[] bytes = blockString.getBytes();
			md.reset();
			md.update(bytes);
			byte[] digest = md.digest();
			return Base64.getEncoder().encodeToString(digest);
		} catch (NoSuchAlgorithmException e) {
	//		should never happen
			e.printStackTrace();
			return "undefined";
		}
	}

	public int getIndex() {
		return index;
	}

	public void setIndex(int index) {
		this.index = index;
	}

	public long getTimestamp() {
		return timestamp;
	}

	public void setTimestamp(long timestamp) {
		this.timestamp = timestamp;
	}

	public List<Transaction> getTransactions() {
		return transactions;
	}

	public void setTransactions(List<Transaction> transactions) {
		this.transactions = transactions;
	}

	public int getProof() {
		return proof;
	}

	public void setProof(int proof) {
		this.proof = proof;
	}

	public String getPreviousHash() {
		return previousHash;
	}

	public void setPreviousHash(String previousHash) {
		this.previousHash = previousHash;
	}
	
}

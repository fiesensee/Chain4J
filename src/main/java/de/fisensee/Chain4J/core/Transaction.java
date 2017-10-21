package de.fisensee.Chain4J.core;

public class Transaction {

	private String sender;
	private String recipient;
	private int amount;
	
	public Transaction(String sender, String recipient, int amount) {
		super();
		this.sender = sender;
		this.recipient = recipient;
		this.amount = amount;
	}

	public String getSender() {
		return sender;
	}

	public void setSender(String sender) {
		this.sender = sender;
	}

	public String getRecipient() {
		return recipient;
	}

	public void setRecipient(String recipient) {
		this.recipient = recipient;
	}

	public int getAmount() {
		return amount;
	}

	public void setAmount(int amount) {
		this.amount = amount;
	}
	
	

}

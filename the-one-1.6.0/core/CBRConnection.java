/* 
 * Copyright 2010 Aalto University, ComNet
 * Released under GPLv3. See LICENSE.txt for details. 
 */
package core;

import routing.EpidemicRouter;
import routing.MessageRouter;
import routing.SprayAndWaitRouter;

/**
 * A constant bit-rate connection between two DTN nodes.
 */
public class CBRConnection extends Connection {
	private int speed;
	private double transferDoneTime;
	private double CTW = 360;
	private double deltaT = 5000;

	/**
	 * Creates a new connection between nodes and sets the connection
	 * state to "up".
	 * @param fromNode The node that initiated the connection
	 * @param fromInterface The interface that initiated the connection
	 * @param toNode The node in the other side of the connection
	 * @param toInterface The interface in the other side of the connection
	 * @param connectionSpeed Transfer speed of the connection (Bps) when 
	 *  the connection is initiated
	 */
	public CBRConnection(DTNHost fromNode, NetworkInterface fromInterface, 
			DTNHost toNode,	NetworkInterface toInterface, int connectionSpeed) {
		super(fromNode, fromInterface, toNode, toInterface);
		this.speed = connectionSpeed;
		this.transferDoneTime = 0;
		fromNode.encounters++;
		if(SimClock.getTime() > CTW) {
			fromNode.encounters = 0;
			CTW = 360 + SimClock.getTime();
		}
		int min=0;
		MessageRouter routerproto;
		MessageRouter oldRouter;
		if(SimClock.getTime() > deltaT) {
			for(int i=0; i<2; i++) {
				if(fromNode.data[i][(int) (fromNode.encounters*500/360.0)] < fromNode.data[min][(int) (fromNode.encounters*500/360.0)]) min = i;
			}
			Settings s = new Settings("Group"+ fromNode.grpid);
			if(min==0) {
				System.out.print("original: " );
				fromNode.getRouter().incomingMessages.entrySet().forEach(entry->{
					System.out.print(fromNode.toString() + entry.getKey() + " " + entry.getValue() + " ");  
				 });
				
				oldRouter = fromNode.getRouter();
				oldRouter.incomingMessages = fromNode.getRouter().incomingMessages;
				System.out.print("copied: ");
				oldRouter.incomingMessages.entrySet().forEach(entry->{
					System.out.print(fromNode.toString() +entry.getKey() + " " + entry.getValue()+ " ");  
				 });
				routerproto = (MessageRouter)s.createIntializedObject("routing." + "EpidemicRouter");
				routerproto.init(fromNode, fromNode.msgListeners);
				routerproto.incomingMessages = oldRouter.incomingMessages;
				routerproto.messages = oldRouter.messages;
				routerproto.deliveredMessages = oldRouter.deliveredMessages;
				routerproto.blacklistedMessages = oldRouter.blacklistedMessages;
				fromNode.router = routerproto;
				System.out.print(fromNode.toString() +"Changed to Epidemic. After change: ");
				fromNode.getRouter().incomingMessages.entrySet().forEach(entry->{
					System.out.print(entry.getKey() + " " + entry.getValue());  
				 });
				 System.out.println();
			}
			else {
				System.out.print("original: " );
				fromNode.getRouter().incomingMessages.entrySet().forEach(entry->{
					System.out.print(fromNode.toString() + entry.getKey() + " " + entry.getValue() + " ");  
				 });
				oldRouter = fromNode.getRouter();
				oldRouter.incomingMessages = fromNode.getRouter().incomingMessages;
				System.out.print("copied: ");
				oldRouter.incomingMessages.entrySet().forEach(entry->{
					System.out.print(fromNode.toString() +entry.getKey() + " " + entry.getValue()+" ");  
				 });
				routerproto = (MessageRouter)s.createIntializedObject("routing." + "SprayAndWaitRouter");
				routerproto.init(fromNode, fromNode.msgListeners);
				routerproto.incomingMessages = oldRouter.incomingMessages;
				routerproto.messages = oldRouter.messages;
				routerproto.deliveredMessages = oldRouter.deliveredMessages;
				routerproto.blacklistedMessages = oldRouter.blacklistedMessages;
				fromNode.router = routerproto;
				System.out.print(fromNode.toString() +"Changed to S&W. After change: ");
				fromNode.getRouter().incomingMessages.entrySet().forEach(entry->{
					System.out.print(entry.getKey() + " " + entry.getValue());  
				 });
				 System.out.println();
			}	
			//fromNode.setRouter(router);
			//System.out.print("Epidemic " + fromNode.data[0][(int) (fromNode.encounters*500/360.0)]);
			//System.out.println(" S&W " + fromNode.data[1][(int) (fromNode.encounters*500/360.0)]);
			//if(min == 0) System.out.println("Epidemic");
			//else System.out.println("S&W");
			deltaT = 5000 + SimClock.getTime();
		}
		//toNode.encounters
		
		//if time > CTW, change density to zero
	}

	/**
	 * Sets a message that this connection is currently transferring. If message
	 * passing is controlled by external events, this method is not needed
	 * (but then e.g. {@link #finalizeTransfer()} and 
	 * {@link #isMessageTransferred()} will not work either). Only a one message
	 * at a time can be transferred using one connection.
	 * @param from The host sending the message
	 * @param m The message
	 * @return The value returned by 
	 * {@link MessageRouter#receiveMessage(Message, DTNHost)}
	 */
	public int startTransfer(DTNHost from, Message m) {
		m.updateProperty("protocol", from.getRouter().getClass().getSimpleName());
		m.updateProperty("density", from.encounters*500/360.0);
		m.updateProperty("overhead", from.getNrofMessages());
		assert this.msgOnFly == null : "Already transferring " + 
			this.msgOnFly + " from " + this.msgFromNode + " to " + 
			this.getOtherNode(this.msgFromNode) + ". Can't " + 
			"start transfer of " + m + " from " + from;

		this.msgFromNode = from;
		Message newMessage = m.replicate();
		//System.out.println(m.toString() + " " + m.getProperty("protocol") + " " +  Math.floor(Double.parseDouble(m.getProperty("density").toString())) + " " + m.getProperty("overhead"));
		int retVal = getOtherNode(from).receiveMessage(newMessage, from);

		if (retVal == MessageRouter.RCV_OK) {
			this.msgOnFly = newMessage;
			this.transferDoneTime = SimClock.getTime() + 
			(1.0*m.getSize()) / this.speed;
		}

		return retVal;
	}

	/**
	 * Aborts the transfer of the currently transferred message.
	 */
	public void abortTransfer() {
		assert msgOnFly != null : "No message to abort at " + msgFromNode;
		getOtherNode(msgFromNode).messageAborted(this.msgOnFly.getId(),
				msgFromNode,getRemainingByteCount());
		clearMsgOnFly();
		this.transferDoneTime = 0;
	}

	/**
	 * Gets the transferdonetime
	 */
	public double getTransferDoneTime() {
		return transferDoneTime;
	}
	
	/**
	 * Returns true if the current message transfer is done.
	 * @return True if the transfer is done, false if not
	 */
	public boolean isMessageTransferred() {
		return getRemainingByteCount() == 0;
	}

	/**
	 * returns the current speed of the connection
	 */
	public double getSpeed() {
		return this.speed;
	}

	/**
	 * Returns the amount of bytes to be transferred before ongoing transfer
	 * is ready or 0 if there's no ongoing transfer or it has finished
	 * already
	 * @return the amount of bytes to be transferred
	 */
	public int getRemainingByteCount() {
		int remaining;

		if (msgOnFly == null) {
			return 0;
		}

		remaining = (int)((this.transferDoneTime - SimClock.getTime()) 
				* this.speed);

		return (remaining > 0 ? remaining : 0);
	}

	/**
	 * Returns a String presentation of the connection.
	 */
	public String toString() {
		return super.toString() + (isTransferring() ?  
				" until " + String.format("%.2f", this.transferDoneTime) : "");
	}

}

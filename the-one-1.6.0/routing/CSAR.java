package routing;

import core.Settings;

public class CSAR extends ActiveRouter {
	
	/**
	 * Constructor. Creates a new message router based on the settings in
	 * the given Settings object.
	 * @param s The settings object
	 */
	public CSAR(Settings s) {
		super(s);
	}
	
	/**
	 * Copy constructor.
	 * @param r The router prototype where setting values are copied from
	 */
	protected CSAR(CSAR r) {
		super(r);
	}
			
	@Override
	public void update() {
		super.update();
		if(choice == 0) {
            if (isTransferring() || !canStartTransfer()) {
                return; // transferring, don't try other connections yet
            }
            
            // Try first the messages that can be delivered to final recipient
            if (exchangeDeliverableMessages() != null) {
                return; // started a transfer, don't try others (yet)
            }
		
            // then try any/all message to any/all connection
            this.tryAllMessagesToAllConnections();
        }
        else {
            if (!canStartTransfer() || isTransferring()) {
                return; // nothing to transfer or is currently transferring 
            }
    
            /* try messages that could be delivered to final recipient */
            if (exchangeDeliverableMessages() != null) {
                return;
            }
            
            /* create a list of SAWMessages that have copies left to distribute */
            @SuppressWarnings(value = "unchecked")
            List<Message> copiesLeft = sortByQueueMode(getMessagesWithCopiesLeft());
            
            if (copiesLeft.size() > 0) {
                /* try to send those messages */
                this.tryMessagesToConnections(copiesLeft, getConnections());
            }
        }
	}
	
	
	@Override
	public CSAR replicate() {
		return new CSAR(this);
	}

}
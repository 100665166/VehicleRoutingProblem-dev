package VRP.messaging;

import VRP.*;

import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.*;
import jade.lang.acl.ACLMessage;

@SuppressWarnings("serial")
public class DeliveryAgent extends Agent
{
	private static final String SEPARATOR = Strings.SEPARATOR;
	
	private AID masterRoutingAgent;
	private int capacity;
	
	private boolean loadArguments = true;
	
	public DeliveryAgent() 
	{
		masterRoutingAgent = new AID(Strings.MRA_NAME, AID.ISLOCALNAME);
		
		// DA capacity related
		capacity = 0;
		
		System.out.println("New Delivery Agent instantiated.");
	}
	
	public DeliveryAgent(boolean loadedFromController) 
	{
		this();

		loadArguments = loadedFromController;
	}
	
	@Override
	protected void setup() 
	{
		// Initialise fields
		if (loadArguments) 
		{
			// load arguments if necessary, not at the moment
			Object[] args = getArguments();
		}
		
		// Initialise behaviours, the ticker behaviours are added and removed in receiveDestination()		
		addBehaviour(new CyclicBehaviour(this) 
		{
			@Override
			public void action() 
			{
				processMessage(receive());
				block();
			}
		});
		
		addBehaviour(new OneShotBehaviour(this) 
		{
			@Override
			public void action() 
			{
		        // To kick things off...
				getDestination();
				block();
			}
		});
	}
	
	// ================================================================================================================================================
	// MESSAGE PROCESSING BLOCK START
	// ================================================================================================================================================
	
	private void processMessage(ACLMessage msg) 
	{
		if (msg != null) 
		{
			System.out.println(getLocalName() + ": Received message from " + msg.getSender().getLocalName() + ", processing...");
			
			if (msg.getPerformative() == ACLMessage.NOT_UNDERSTOOD) 
			{
				return;
			}
			
			String body = msg.getContent();
			
			// From MRA's sendDestination
			if (body.startsWith(Strings.MRA_SEND_DESTINATION_COMMAND)) 
			{
				addBehaviour(new OneShotBehaviour() 
				{
					@Override
					public void action() 
					{
						receiveDestination(msg);
					}
				});
			}
			else 
			{
				System.out.println(getLocalName() + ": Did not understand message received. Sender: " + msg.getSender().getName() + "; Content: " + msg.getContent());
				
				addBehaviour(new OneShotBehaviour() 
				{
					@Override
					public void action() 
					{
						Utilities.sendReply(this.myAgent, msg, ACLMessage.NOT_UNDERSTOOD, "");
					}
				});
			}
		}
	}
	
	// Obtain new delivery order from MRA
	// TO: MRA, Request, getDestination
	// RE: AGREE/REFUSE, bestArrangement
	private void getDestination() 
	{
		int vehicleNumber = Integer.parseInt(getLocalName().substring(getLocalName().indexOf("#") + 1));
		capacity = VRP_Test3.params.vehicleCapacities[vehicleNumber];
		
		System.out.println(getLocalName() + ": Requesting next location. My capacity is " + capacity + ".");
		
		Utilities.sendMessage(this, ACLMessage.REQUEST, masterRoutingAgent, Strings.DA_GET_DESTINATION_COMMAND + SEPARATOR + capacity);
	}
	
	// Obtained route from MRA
	// DA begins dispatch according to route 
	private void receiveDestination(ACLMessage msg) 
	{
		String[] body = msg.getContent().split(SEPARATOR);
		
		if (msg.getPerformative() == ACLMessage.AGREE) 
		{		
			System.out.println(getLocalName() + ": Obtained route from " + msg.getSender().getLocalName() + ". Beginning dispatch along route -> " + body[1]);
		}
		else if (msg.getPerformative() == ACLMessage.REFUSE) 
		{
			if (body[1].equals(Strings.DONE)) 
			{
				doneDelivering();
			} 
			else {}
		}
	}
	
	// Let MRA know that the DA has completed their delivery 
	private void doneDelivering() 
	{
		Utilities.sendMessage(this, ACLMessage.INFORM, masterRoutingAgent, Strings.DA_DELIVER_COMMAND);
		System.out.println(getLocalName() + ": Done delivering.");
	}
}
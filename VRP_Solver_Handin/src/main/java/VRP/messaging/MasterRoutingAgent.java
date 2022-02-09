package VRP.messaging;

import VRP.*;

import java.util.ArrayList;
import java.util.List;

import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.*;
import jade.lang.acl.ACLMessage;

@SuppressWarnings("serial")
public class MasterRoutingAgent extends Agent
{
	private static final String SEPARATOR = Strings.SEPARATOR;
	
	private List<DeliveryAgentData> deliveryAgents;
	
	public MasterRoutingAgent() 
	{
		deliveryAgents = new ArrayList<DeliveryAgentData>();

		System.out.println(Strings.MRA_NAME + ": Master Routing Agent has been instantiated.");
	}

	@Override
	protected void setup() 
	{
		// Add message handler
		addBehaviour(new CyclicBehaviour(this) 
		{
			@Override
			public void action() 
			{
				processMessage(receive());
				block();
			}
		});
	}
	
	// ================================================================================================================================================
	// MESSAGE PROCESSING BLOCK START
	// ================================================================================================================================================
	
	public void processMessage(ACLMessage msg) 
	{
		if (msg != null) 
		{
			if (msg.getPerformative() == ACLMessage.NOT_UNDERSTOOD) 
			{
				return;
			}
			
			String content = msg.getContent();
			
			// DA requests for a new delivery
			// Response from MRA required? Yes (AGREE/REFUSE, sendDestination)
			if (content.startsWith(Strings.DA_GET_DESTINATION_COMMAND)) 
			{
				String[] body = content.split(SEPARATOR);
				
				System.out.println(Strings.MRA_NAME + ": Sending next location in route to " + msg.getSender().getLocalName() + ". Your capacity is " + body[1] +".");
				
				addBehaviour(new OneShotBehaviour() 
				{
					@Override
					public void action() 
					{
						sendDestination(msg);
					}
				});
			} 
			// DA sends confirmation that it has dropped off a package
			// Response from MRA required? Yes (confirmDeliver)
			else if (content.startsWith(Strings.DA_DELIVER_COMMAND)) 
			{
				System.out.println(Strings.MRA_NAME + ": Confirming to " + msg.getSender().getLocalName() + ", received delivery confirmation...");
				
				addBehaviour(new OneShotBehaviour() 
				{
					@Override
					public void action() 
					{
						confirmDeliver(msg);
					}
				});
			} 
			else 
			{
				System.out.println(Strings.MRA_NAME + ": Did not understand message received. Sender: " + msg.getSender().getName() + "; Content: " + msg.getContent());
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
	
	// ================================================================================================================================================
	// MESSAGE PROCESSING BLOCK END
	// ================================================================================================================================================
	
	private DeliveryAgentData findDeliveryAgent(AID id) 
	{
		for (DeliveryAgentData daData : deliveryAgents) 
		{
			if (daData.id.equals(id)) 
			{
				return daData;
			}
		}
		
		return null;
	}
	
	private void sendDestination(ACLMessage msg) 
	{
		AID deliveryAgent = msg.getSender();
		int vehicleNumber = Integer.parseInt(deliveryAgent.getLocalName().substring(deliveryAgent.getLocalName().indexOf("#") + 1));
		DeliveryAgentData daData = findDeliveryAgent(deliveryAgent);
		
		System.out.println(Strings.MRA_NAME + ": Calculating route for " + deliveryAgent.getLocalName() + "...");
		
		// Generate a route
		VRP_Test3.MRA_Function(VRP_Test3.params.indSupCap, VRP_Test3.params.vehicles);
		
		// Now store it
		int[][] routes = VRP_Test3.bestArrangement;
		
		// We only want the route that matches AID
		int[] p = routes[vehicleNumber];
		
	    if (p == null) 
		{
			System.out.println(Strings.MRA_NAME + ": New route unavailable.");
			
			// DA has already completed route
			Utilities.sendReply(this, msg, ACLMessage.REFUSE, Strings.MRA_SEND_DESTINATION_COMMAND + SEPARATOR + Strings.DONE);
		}
		else 
		{
			// Now convert it to a string so that we can pass it onto the DA properly
	        StringBuilder routeSb = new StringBuilder();

			System.out.print(Strings.MRA_NAME + ": Route for " + deliveryAgent.getLocalName() + " is -> ");
			for(int i = 0; i != p.length; i++) 
			{
				System.out.print(p[i] + " ");
				routeSb.append(p[i] + " ");
			}
			System.out.println();
			
	        String route = routeSb.toString();
			
			// Send route to DA
			Utilities.sendReply(this, msg, ACLMessage.AGREE, Strings.MRA_SEND_DESTINATION_COMMAND + SEPARATOR + route);
		}
	}
	
	private void confirmDeliver(ACLMessage msg) 
	{
		AID deliveryAgent = msg.getSender();
		DeliveryAgentData daData = findDeliveryAgent(deliveryAgent);

		String[] contents = msg.getContent().split(SEPARATOR);
		
		if (contents[0].equals(Strings.DA_DELIVER_COMMAND)) 
		{
			Utilities.sendReply(this, msg, ACLMessage.CONFIRM, Strings.MRA_CONFIRM_DELIVER_COMMAND);
		}
	}
}
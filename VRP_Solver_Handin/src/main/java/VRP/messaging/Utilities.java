package VRP.messaging;

import jade.core.AID;
import jade.core.Agent;
import jade.lang.acl.ACLMessage;

public class Utilities
{
	public static void sendMessage(Agent sender, int perf, AID receiver, String content) 
	{
		ACLMessage msg = new ACLMessage(perf);
		msg.addReceiver(receiver);
		msg.setContent(content);
		sender.send(msg);
	}
	
	public static void sendReply(Agent sender, ACLMessage msg, int perf, String content) 
	{
		ACLMessage reply = msg.createReply();
		reply.setPerformative(perf);
		reply.setContent(content);
		sender.send(reply);
	}
}
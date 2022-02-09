package VRP.messaging;

import java.util.Queue;

import jade.core.AID;

public class DeliveryAgentData
{
	public AID id;
	public int capacity;
	public Queue<int[][]> route;
}
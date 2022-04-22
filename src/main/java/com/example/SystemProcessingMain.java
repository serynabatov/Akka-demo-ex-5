package com.example;


import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;

import java.io.File;
import java.io.IOException;

/*
 * Clustering
 *
 * Akka clustering offers a membership service
 * - Decentralized
 * - No single point of failure / bottleneck
 *
 * Implementation
 * - Peer to peer
 * - Gossip protocol
 * - Automatic failure detection
 *
 * Node: a logical member of a cluster
 * - There can be multiple nodes on each physical machine
 * - Each node is identified by a tuple hostname:port:uid
 * - You can think each node to be a process / actor system
 *
 * Cluster: a set of nodes joined together through the membership service
 *
 * Leader: a role in the cluster
 * - A single node in the cluster acts as a leader
 * - The leader manages cluster convergence and membership state transitions
 *
 * Clustering basics
 * - An Akka application can be distributed over a cluster
 * - Each node hosts some part of the application
 * - Cluster membership and the actors running on a member node are decoupled
 * - A node can be a member of a cluster hosting any number of actors
 * - An actor system joins a cluster by sending a join command to one of the nodes in the cluster
 *
 * Clustering protocol
 * - Nodes organize themselves into an overlay network
 * - They distribute information about cluster members using a gossip protocol
 *   - Nodes propagate messages containing their current view of the membership
 *   - Nodes update their view based on the received messages
 *     - Messages are designed in such a way that the state of nodes eventually converges
 *     - They contain vector clocks to record a partial order of the events (nodes joining, leaving, …)
 *        observed in the environment
 *   - Information about the cluster converges at a given node when the node can prove that the cluster state
 *      it is observing has been observed by all other nodes in the cluster
 *   - Gossip convergence cannot occur while some node is "unreachable"
 *      – A state in the lifecycle of nodes indicating that it is not currently possible to communicate with the node
 *      - The node need to become reachable again or be removed from the cluster
 *   - Each node N is monitored by a few other nodes (Using periodic heartbeat)
 *   - If one of these nodes cannot reach node N for a while it marks node N as unreachable
 *   - When a node is unreachable, a network partition is possible
 *      - No membership actions can be taken until the node becomes reachable again or goes down
 * - One node is marked as leader
 *    - No explicit leader election
 *    - All the nodes will agree on the leader when the gossip state converges
 * - The leader can shift members in and out of the cluster
 *    - Leader actions are possible only after gossip convergence
 *    - But you can configure the system to allow the leader to automatically remove unreachable nodes after some time
 *      - Possibly dangerous in the case of temporary network partitions
 *      - The node might come back online
 *
 * Seed nodes
 * - Seed nodes are configured contact points for new nodes that want to join the cluster
 * - When a new node wants to join the cluster
 *   - It contacts all the seed nodes
 *     - At least one needs to be active
 *   - It sends a join command to the first seed nodes that answers
 *
 * Cluster tools
 * - Akka offers higher-level tools that build on top of clustering
 *   - Cluster singleton: to ensure that a single actor of a certain type exists in the cluster
 *   - Cluster sharding: distributes actors across nodes of the cluster:
 *      - Ensuring that they can communicate without knowing their physical location
 *   - Distributed data: creates a distributed key-value store across the nodes of the cluster
 *   - Cluster metrics: to publish and collect health metrics about cluster members
 *
 * Distributed publish-subscribe
 * - Distributed pub-sub decouples senders and receivers
 *   - Receivers subscribe to topics
 *   - Senders publish to topics
 *     - They need not know the receiver identity or the nodes they are running on
 * - Implemented using a mediator actor
 *   - Started on all nodes in the cluster
 *   - Contains a registry that associates nodes to their topics of interest
 *   - The registry is replicated across mediators
 *     - Changes are propagated with eventual consistency
 * - Efficient message dispatching
 *   – Messages are delivered to each node (that has subscribed actors) only once
 *   - and then delivered to all local subscribers
 *  */
public class SystemProcessingMain {

    public static void main(String[] args) throws InterruptedException, IOException {
        final ActorSystem sys = ActorSystem.create("System");
        final ActorRef avgRef = sys.actorOf(AvgOperatorActor.props(), "avgOperator");
        final ActorRef finalClient = sys.actorOf(FinalClient.props(), "finalReceiver");

        // Send messages from multiple threads in parallel
        //tell means “fire-and-forget”, e.g. send a message asynchronously and return immediately.
        avgRef.tell(new SensorDataMessage("S1", 2), finalClient);
        avgRef.tell(new SensorDataMessage("S1", 3), finalClient);
        avgRef.tell(new SensorDataMessage("S1", 4), finalClient);
        avgRef.tell(new SensorDataMessage("S1", 4), finalClient);
        avgRef.tell(new SensorDataMessage("S1", 5), finalClient);
        avgRef.tell(new SensorDataMessage("S2", 5), finalClient);
        avgRef.tell(new SensorDataMessage("S2", 5), finalClient);
        avgRef.tell(new SensorDataMessage("S2", 5), finalClient);

        // Wait for all messages to be sent and received
        sys.terminate();
    }

}

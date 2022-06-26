# Akka-demo-ex-5

- Write a simple stream processing system
- Pipeline of three operators
  - Each message contains a key-value pair
  – There are multiple instances of each operator
    - The key space is partitioned across instances
  - Each operator is defined using
    - A window (size, slide)
    - An aggregation function to process values in the current window
  – When the window is complete, the operator
    - Computes the aggregate value
    - Forwards the value to the next operator
  – It is possible to change the key when forwarding the value
- Guarantee that a value is processed at least once

# How To Run

You need to open the project in **IntelliIDEA** and run the main function in **SystemProcessingMain.java** file

# Architecture

![1 drawio](https://user-images.githubusercontent.com/23494724/175805686-cf8d6ab6-45c1-4fd6-b68e-03e3e711173d.png)

Every operator has n instances from 1 to the number of REPLICAS specified in the pipeline. 

To send messages with at-least-once delivery semantics to destinations every operator instance extend the AbstractPersistentActorWithAtLeastOnceDelivery 
class instead of AbstractPersistentActor on the sending side. 
It takes care of re-sending messages when they have not been confirmed within a configurable timeout.

The state of the sending actor, including which messages have been sent that have not been confirmed by the recipient must be persistent so that it can survive a crash of the sending actor or JVM. 
The AbstractPersistentActorWithAtLeastOnceDelivery class does not persist anything by itself. For this purposes we use the journals and there is a possibility to use the snapshotoffer.

At-least-once delivery implies that original message sending order is not always preserved, and the destination may receive duplicate messages. 

The deliver method is used to send a message to a destination. The confirmDelivery method is used when the destination has replied with a confirmation message.

To send messages to the destination path, we have used the deliver method after we have persisted the intent to send the message.

The destination actor must send back a confirmation message. When the sending actor receives this confirmation message we have persisted the fact that the message was delivered successfully and then call the confirmDelivery method.

If the persistent actor is not currently recovering, the deliver method will send the message to the destination actor. When recovering, messages will be buffered until they have been confirmed using confirmDelivery. Once recovery has completed, if there are outstanding messages that have not been confirmed (during the message replay), the persistent actor will resend these before sending any other messages.

Deliver requires a deliveryIdToMessage function to pass the provided deliveryId into the message so that the correlation between deliver and confirmDelivery is possible. The deliveryId must do the round trip. Upon receipt of the message, the destination actor will send the samedeliveryId wrapped in a confirmation message back to the sender. The sender will then use it to call confirmDelivery method to complete the delivery routine.


# Output

In our tests we send 10 messages and do it three times:
- Without failure
- With failure when the messages didn't pass the first stage
- With failure when the messages passed the first stage

Since it is AtLeastOnceDelivery if the messages passed the first stage since every actor is recovering from the failure, there will be the duplicates of these messages in the output.

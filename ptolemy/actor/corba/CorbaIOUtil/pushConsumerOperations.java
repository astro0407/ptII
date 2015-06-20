package ptolemy.actor.corba.CorbaIOUtil;

/**
 * ptolemy/actor/corba/CorbaIOUtil/pushConsumerOperations.java .
 * Generated by the IDL-to-Java compiler (portable), version "3.1"
 * from CorbaIO.idl
 * Wednesday, April 16, 2003 5:05:14 PM PDT
 */

/* A CORBA compatible interface for a push consumer.
 */
public interface pushConsumerOperations {
    /* this method is intended to be called remotely by a push publisher,
     * so that data can be delived over the network to a push consumer.
     */
    void push(org.omg.CORBA.Any data)
            throws ptolemy.actor.corba.CorbaIOUtil.CorbaIllegalActionException;
} // interface pushConsumerOperations
package com.amazonaws.samples;

import javax.jms.Connection;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageConsumer;
import javax.jms.MessageListener;
import javax.jms.MessageProducer;
import javax.jms.Session;
import javax.jms.TextMessage;

import org.apache.activemq.ActiveMQSslConnectionFactory;

public class Bootstrap {

    private ActiveMQSslConnectionFactory connFact;
    private Connection conn;
    private Session session;
    private MessageConsumer consumer;

    public Bootstrap() throws JMSException {
        connFact = new ActiveMQSslConnectionFactory(System.getenv("AMAZONMQ_URL"));
        connFact.setConnectResponseTimeout(10000);

        conn = connFact.createConnection(System.getenv("AMAZONMQ_USER"), System.getenv("AMAZONMQ_PASSWORD"));
        conn.setClientID("sample-jms-client");
        conn.start();

        session = conn.createSession(false, Session.CLIENT_ACKNOWLEDGE);

        consumer = session.createConsumer(session.createQueue("DEMO.QUEUE1"));
    }

    public static void main(String... args) throws Exception {
        Bootstrap bootstrap = new Bootstrap();
        bootstrap.run();
    }

    public void run() throws JMSException {
        consumer.setMessageListener(new MessageListener() {
            public void onMessage(Message message) {
                try {
                    System.out.println(
                        String.format("received message with correlation id '%s', message id '%s' and replyTo '%s'",
                            message.getJMSCorrelationID(), 
                            message.getJMSMessageID(), 
                            message.getJMSReplyTo()));

                    TextMessage responseMsg = session.createTextMessage(String.format("Current time in millis is %s", System.currentTimeMillis()));
                    responseMsg.setJMSCorrelationID(message.getJMSCorrelationID());

                    MessageProducer producer = session.createProducer(message.getJMSReplyTo());
                    producer.send(responseMsg);
                    message.acknowledge();

                    System.out.println(
                        String.format("response message sent to '%s'",
                            message.getJMSReplyTo()));

                    producer.close();
                } catch (JMSException e) {
                    throw new RuntimeException(e);
                }
            }
        });
    }
}
package com.bytechef.ee.message.broker.aws.sevice;

import com.bytechef.ee.message.broker.aws.util.AwsMessage;
import org.springframework.stereotype.Service;

@Service
public class AwsMessageSender {
//    private final SqsTemplate sqsTemplate;

    public AwsMessageSender(Object sqsTemplate) {
//        this.sqsTemplate = sqsTemplate;
    }

    public void publish(String queueName, AwsMessage message) {
//        sqsTemplate.send(to -> to.queue(queueName).payload(message));
    }
}

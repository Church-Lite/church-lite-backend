package com.smartverse.churchlitebackend.config.rabbit;

import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
public class RabbitConsumer {

    @RabbitListener(queues = "${rabbit.queue.name}")
    public void receiveMessage(Object message){
        System.out.println(message);
    }
}

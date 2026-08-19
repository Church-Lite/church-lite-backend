package com.smartverse.churchlitebackend.config.messaging;

import com.smartverse.churchlitebackend_gen.messaging.RabbitConfig;
import com.smartverse.churchlitebackend_gen.messaging.RabbitExchange;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.nio.charset.StandardCharsets;

@Configuration
@RabbitExchange("smart.church.events")
public class ChurchRabbitConfig extends RabbitConfig {
    private static final String EXCHANGE = "smart.church.events";
    private static final String PAYMENT_EXCHANGE = "smart.payment.events";
    private static final String ROUTING_KEY = "payment.confirmed.CHURCH_LITE";
    private static final String MEMBER_IMAGE_ROUTING_KEY = "social.profile.image.updated";

    @Override
    protected String resolveExchangeName() {
        return EXCHANGE;
    }

    @Bean
    @Override
    public MessageConverter jsonMessageConverter() {
        var delegate = new Jackson2JsonMessageConverter();
        return new MessageConverter() {
            @Override
            public Message toMessage(Object object, MessageProperties properties) {
                return delegate.toMessage(object, properties);
            }

            @Override
            public Object fromMessage(Message message) {
                return new String(message.getBody(), StandardCharsets.UTF_8);
            }
        };
    }

    @Bean
    public Queue paymentConfirmedQueue(
            @Value("${smart-payment.confirmed-queue}") String queueName) {
        return new Queue(queueName, true);
    }

    @Bean
    public TopicExchange paymentTopicExchange() {
        return new TopicExchange(PAYMENT_EXCHANGE);
    }

    @Bean
    public Binding paymentConfirmedBinding(
            Queue paymentConfirmedQueue,
            @Qualifier("paymentTopicExchange") TopicExchange paymentTopicExchange) {
        return BindingBuilder.bind(paymentConfirmedQueue)
                .to(paymentTopicExchange)
                .with(ROUTING_KEY);
    }

    @Bean
    public Queue socialProfileImageUpdatedQueue(
            @Value("${CHURCH_MEMBER_IMAGE_QUEUE:smart.church.member-image.church-lite}") String queueName) {
        return new Queue(queueName, true);
    }

    @Bean
    public Binding socialProfileImageUpdatedBinding(
            Queue socialProfileImageUpdatedQueue,
            TopicExchange appTopicExchange) {
        return BindingBuilder.bind(socialProfileImageUpdatedQueue)
                .to(appTopicExchange)
                .with(MEMBER_IMAGE_ROUTING_KEY);
    }
}

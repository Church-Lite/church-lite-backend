package com.smartverse.churchlitebackend.config.messaging;
import com.smartverse.churchlitebackend_gen.messaging.*;import org.springframework.amqp.core.*;import org.springframework.amqp.support.converter.*;import org.springframework.amqp.core.Message;import org.springframework.context.annotation.*;import java.nio.charset.StandardCharsets;
@Configuration @RabbitExchange("smart.payment.events") public class PaymentRabbitConfig extends RabbitConfig{
 @Override protected String resolveExchangeName(){return "smart.payment.events";}
 @Bean Queue paymentConfirmedQueue(){return new Queue("smart.payment.confirmed.church-lite",true);}
 @Bean Binding paymentConfirmedBinding(TopicExchange appTopicExchange){return BindingBuilder.bind(paymentConfirmedQueue()).to(appTopicExchange).with("payment.confirmed.CHURCH_LITE");}
 @Override @Bean public MessageConverter jsonMessageConverter(){var jackson=new Jackson2JsonMessageConverter();return new MessageConverter(){public Message toMessage(Object object,org.springframework.amqp.core.MessageProperties properties){return jackson.toMessage(object,properties);}public Object fromMessage(Message message){return new String(message.getBody(),StandardCharsets.UTF_8);}};}
}

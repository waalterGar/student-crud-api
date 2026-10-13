package com.students.crud_students.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.Map;

@Configuration
public class RabbitMQConfig {

    @Value("${rabbitmq.exchange.name:student.exchange}")
    private String exchangeName;

    @Value("${rabbitmq.queue.notification.name:student.notification.queue}")
    private String notificationQueueName;

    @Value("${rabbitmq.queue.email.name:student.email.queue}")
    private String emailQueueName;

    @Value("${rabbitmq.routing.key:student.created}")
    private String routingKey;

    public static final String DLX_NAME = "student.dlx";
    public static final String NOTIFICATION_DLQ_NAME = "student.notification.dlq";
    public static final String EMAIL_DLQ_NAME = "student.email.dlq";

    public static final String NOTIFICATION_DLQ_ROUTING_KEY = "student.notification.dead";
    public static final String EMAIL_DLQ_ROUTING_KEY = "student.email.dead";

    @Bean
    public TopicExchange studentExchange() {
        return new TopicExchange(exchangeName);
    }

    @Bean
    public TopicExchange deadLetterExchange() {
        return new TopicExchange(DLX_NAME);
    }

    @Bean
    public Queue notificationDeadLetterQueue() {
        return QueueBuilder.durable(NOTIFICATION_DLQ_NAME).build();
    }

    @Bean
    public Queue emailDeadLetterQueue() {
        return QueueBuilder.durable(EMAIL_DLQ_NAME).build();
    }

    @Bean
    public Binding notificationDlqBinding() {
        return BindingBuilder
                .bind(notificationDeadLetterQueue())
                .to(deadLetterExchange())
                .with(NOTIFICATION_DLQ_ROUTING_KEY);
    }

    @Bean
    public Binding emailDlqBinding() {
        return BindingBuilder
                .bind(emailDeadLetterQueue())
                .to(deadLetterExchange())
                .with(EMAIL_DLQ_ROUTING_KEY);
    }

    @Bean
    public Queue notificationQueue() {
        Map<String, Object> args = new HashMap<>();
        args.put("x-dead-letter-exchange", DLX_NAME);
        args.put("x-dead-letter-routing-key", NOTIFICATION_DLQ_ROUTING_KEY);

        return QueueBuilder.durable(notificationQueueName)
                .withArguments(args)
                .build();
    }

    @Bean
    public Queue emailQueue() {
        Map<String, Object> args = new HashMap<>();
        args.put("x-dead-letter-exchange", DLX_NAME);
        args.put("x-dead-letter-routing-key", EMAIL_DLQ_ROUTING_KEY);

        return QueueBuilder.durable(emailQueueName)
                .withArguments(args)
                .build();
    }

    @Bean
    public Binding notificationBinding() {
        return BindingBuilder
                .bind(notificationQueue())
                .to(studentExchange())
                .with(routingKey);
    }

    @Bean
    public Binding emailBinding() {
        return BindingBuilder
                .bind(emailQueue())
                .to(studentExchange())
                .with(routingKey);
    }

    @Bean
    public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }
}
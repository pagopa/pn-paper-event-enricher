package it.pagopa.pn.paper.event.enricher.config.springbootcfg;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.awspring.cloud.autoconfigure.sqs.SqsProperties;
import io.awspring.cloud.sqs.config.SqsMessageListenerContainerFactory;
import io.awspring.cloud.sqs.listener.SqsContainerOptionsBuilder;
import io.awspring.cloud.sqs.listener.errorhandler.AsyncErrorHandler;
import io.awspring.cloud.sqs.listener.errorhandler.ErrorHandler;
import io.awspring.cloud.sqs.listener.interceptor.AsyncMessageInterceptor;
import io.awspring.cloud.sqs.listener.interceptor.MessageInterceptor;
import io.awspring.cloud.sqs.support.converter.MessagingMessageConverter;
import io.awspring.cloud.sqs.support.converter.SqsMessagingMessageConverter;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.PropertyMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.services.sqs.SqsAsyncClient;

import java.time.Duration;

@Configuration
public class CustomContainerFactoryConfig {

    private final long listenerShutdownTimeout;
    private final SqsProperties sqsProperties;

    public CustomContainerFactoryConfig(@Value("${pn.paper-event-enricher.sqs.archive-queue-stop-timeout}") long listenerShutdownTimeout, SqsProperties sqsProperties) {
        this.listenerShutdownTimeout = listenerShutdownTimeout;
        this.sqsProperties = sqsProperties;
    }

    @Bean
    SqsMessageListenerContainerFactory<Object> defaultSqsListenerContainerFactory(ObjectProvider<SqsAsyncClient> sqsAsyncClient, ObjectProvider<AsyncErrorHandler<Object>> asyncErrorHandler,
                                                                                  ObjectProvider<ErrorHandler<Object>> errorHandler,
                                                                                  ObjectProvider<AsyncMessageInterceptor<Object>> asyncInterceptors,
                                                                                  ObjectProvider<MessageInterceptor<Object>> interceptors, ObjectProvider<ObjectMapper> objectMapperProvider,
                                                                                  MessagingMessageConverter<?> messagingMessageConverter) {

        return this.buildDefaultProps(sqsAsyncClient, asyncErrorHandler, errorHandler, asyncInterceptors, interceptors, objectMapperProvider, messagingMessageConverter);
    }

    @Bean
    SqsMessageListenerContainerFactory<Object> customSqsListenerContainerFactory(ObjectProvider<SqsAsyncClient> sqsAsyncClient, ObjectProvider<AsyncErrorHandler<Object>> asyncErrorHandler,
                                                                                 ObjectProvider<ErrorHandler<Object>> errorHandler,
                                                                                 ObjectProvider<AsyncMessageInterceptor<Object>> asyncInterceptors,
                                                                                 ObjectProvider<MessageInterceptor<Object>> interceptors, ObjectProvider<ObjectMapper> objectMapperProvider,
                                                                                 MessagingMessageConverter<?> messagingMessageConverter) {

        SqsMessageListenerContainerFactory<Object> factory = this.buildDefaultProps(sqsAsyncClient, asyncErrorHandler, errorHandler, asyncInterceptors, interceptors, objectMapperProvider, messagingMessageConverter);
        factory.configure(optionsBuilder -> optionsBuilder.listenerShutdownTimeout(Duration.ofMillis(this.listenerShutdownTimeout)));
        return factory;
    }


    // Vedi io.awspring.cloud.autoconfigure.sqs.SqsAutoConfiguration#defaultSqsListenerContainerFactory
    private SqsMessageListenerContainerFactory<Object> buildDefaultProps(ObjectProvider<SqsAsyncClient> sqsAsyncClient, ObjectProvider<AsyncErrorHandler<Object>> asyncErrorHandler,
                                                             ObjectProvider<ErrorHandler<Object>> errorHandler,
                                                             ObjectProvider<AsyncMessageInterceptor<Object>> asyncInterceptors,
                                                             ObjectProvider<MessageInterceptor<Object>> interceptors, ObjectProvider<ObjectMapper> objectMapperProvider,
                                                             MessagingMessageConverter<?> messagingMessageConverter) {

        SqsMessageListenerContainerFactory<Object> factory = new SqsMessageListenerContainerFactory<>();
        factory.configure(this::configureProperties);
        sqsAsyncClient.ifAvailable(factory::setSqsAsyncClient);
        asyncErrorHandler.ifAvailable(factory::setErrorHandler);
        errorHandler.ifAvailable(factory::setErrorHandler);
        interceptors.forEach(factory::addMessageInterceptor);
        asyncInterceptors.forEach(factory::addMessageInterceptor);
        objectMapperProvider.ifAvailable(om -> setMapperToConverter(messagingMessageConverter, om));
        factory.configure(options -> options.messageConverter(messagingMessageConverter));
        return factory;

    }

    private void setMapperToConverter(MessagingMessageConverter<?> messagingMessageConverter, ObjectMapper om) {
        if (messagingMessageConverter instanceof SqsMessagingMessageConverter sqsConverter) {
            sqsConverter.setObjectMapper(om);
        }
    }

    private void configureProperties(SqsContainerOptionsBuilder options) {
        PropertyMapper mapper = PropertyMapper.get().alwaysApplyingWhenNonNull();
        mapper.from(this.sqsProperties.getQueueNotFoundStrategy()).to(options::queueNotFoundStrategy);
        mapper.from(this.sqsProperties.getListener().getMaxConcurrentMessages()).to(options::maxConcurrentMessages);
        mapper.from(this.sqsProperties.getListener().getMaxMessagesPerPoll()).to(options::maxMessagesPerPoll);
        mapper.from(this.sqsProperties.getListener().getPollTimeout()).to(options::pollTimeout);
    }

}

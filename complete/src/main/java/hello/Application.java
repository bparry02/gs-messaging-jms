package hello;

import java.util.Properties;

import javax.jms.ConnectionFactory;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.Session;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.jms.annotation.EnableJms;
import org.springframework.jms.config.JmsListenerContainerFactory;
import org.springframework.jms.config.SimpleJmsListenerContainerFactory;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.jms.core.MessageCreator;
import org.springframework.jms.support.destination.DestinationResolver;
import org.springframework.jms.support.destination.JndiDestinationResolver;

@SpringBootApplication
@EnableJms
public class Application {

    private static final Logger LOG = LoggerFactory
            .getLogger(Application.class);

    private static final String DEFAULT_CONNECTION_FACTORY = "jms/RemoteConnectionFactory";
    public static final String DEFAULT_DESTINATION = "java:/jms/helloBoot";
    private static final String DEFAULT_USERNAME = "boot";
    private static final String DEFAULT_PASSWORD = "bootUser1!";
    private static final String INITIAL_CONTEXT_FACTORY = "org.jboss.naming.remote.client.InitialContextFactory";
    private static final String PROVIDER_URL = "remote://localhost:4447";

    @Bean
    JmsListenerContainerFactory<?> myJmsContainerFactory(
            ConnectionFactory connectionFactory,
            DestinationResolver destinationResolver) {
        SimpleJmsListenerContainerFactory factory = new SimpleJmsListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory);
        factory.setDestinationResolver(destinationResolver);
        return factory;
    }

    @Bean
    DestinationResolver destinationResolver(Properties jndiEnv) {
        JndiDestinationResolver resolver = new JndiDestinationResolver();
        resolver.setJndiEnvironment(jndiEnv);
        return resolver;
    }

    @Bean
    Properties jndiEnv() {
        Properties env = new Properties();
        env.put(Context.INITIAL_CONTEXT_FACTORY, INITIAL_CONTEXT_FACTORY);
        env.put(Context.PROVIDER_URL,
                System.getProperty(Context.PROVIDER_URL, PROVIDER_URL));
        env.put(Context.SECURITY_PRINCIPAL,
                System.getProperty("username", DEFAULT_USERNAME));
        env.put(Context.SECURITY_CREDENTIALS,
                System.getProperty("password", DEFAULT_PASSWORD));
        // NOTE: also chose to disable security in hornetq so that this user can
        // connect and is authorized to publish.
        return env;
    }

    @Bean
    InitialContext initialContext(Properties jndiEnv) {
        try {
            return new InitialContext(jndiEnv);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    @Bean
    ConnectionFactory connectionFactory(InitialContext context) {
        try {
            LOG.info("looking up ConnectionFactory....");
            return (ConnectionFactory) context
                    .lookup(DEFAULT_CONNECTION_FACTORY);
        } catch (NamingException e) {
            e.printStackTrace();
            return null;
        }
    }

    public static void main(String[] args) {
        // Clean out any ActiveMQ data from a previous run
        // FileSystemUtils.deleteRecursively(new File("activemq-data"));

        // Launch the application
        ConfigurableApplicationContext context = SpringApplication.run(
                Application.class, args);

        // Send a message
        MessageCreator messageCreator = new MessageCreator() {
            @Override
            public Message createMessage(Session session) throws JMSException {
                return session.createTextMessage("ping!");
            }
        };
        JmsTemplate jmsTemplate = context.getBean(JmsTemplate.class);
        System.out.println("Sending a new message.");
        jmsTemplate.send(DEFAULT_DESTINATION, messageCreator);
    }

}

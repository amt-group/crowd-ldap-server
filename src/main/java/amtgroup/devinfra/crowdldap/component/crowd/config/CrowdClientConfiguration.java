package amtgroup.devinfra.crowdldap.component.crowd.config;

import com.atlassian.crowd.integration.rest.service.factory.RestCrowdClientFactory;
import com.atlassian.crowd.service.client.ClientProperties;
import com.atlassian.crowd.service.client.ClientPropertiesImpl;
import com.atlassian.crowd.service.client.CrowdClient;
import com.atlassian.crowd.service.factory.CrowdClientFactory;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Properties;

/**
 * @author Vitaly Ogoltsov
 */
@Configuration
@EnableConfigurationProperties(CrowdClientProperties.class)
public class CrowdClientConfiguration {

    @SuppressWarnings("SpellCheckingInspection")
    @Bean
    public ClientProperties crowdClientProperties(CrowdClientProperties crowdClientProperties) {
        Properties properties = new Properties();
        if (crowdClientProperties.getServer() != null) {
            properties.put("crowd.server.url", crowdClientProperties.getServer().getUrl());
        }
        if (crowdClientProperties.getApplication() != null) {
            properties.put("application.name", crowdClientProperties.getApplication().getName());
            properties.put("application.password", crowdClientProperties.getApplication().getPassword());
            properties.put("application.login.url", crowdClientProperties.getApplication().getLoginUrl());
        }
        if (crowdClientProperties.getSession() != null) {
            properties.put("session.validationinterval", String.valueOf(crowdClientProperties.getSession().getValidationInterval()));
            properties.put("session.lastvalidation", crowdClientProperties.getSession().getLastValidation());
            properties.put("session.isauthenticated", crowdClientProperties.getSession().getIsAuthenticated());
            properties.put("session.tokenkey", crowdClientProperties.getSession().getTokenKey());
        }
        return ClientPropertiesImpl.newInstanceFromProperties(properties);
    }

    @Bean
    public CrowdClientFactory crowdClientFactory() {
        return new RestCrowdClientFactory();
    }

    @Bean
    public CrowdClient crowdClient(CrowdClientFactory crowdClientFactory, ClientProperties crowdClientProperties) {
        return crowdClientFactory.newInstance(crowdClientProperties);
    }

}

package amtgroup.devinfra.crowdldap.component.crowd.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * @author Vitaly Ogoltsov
 */
@Configuration
@EnableConfigurationProperties(CrowdLdapProperties.class)
public class CrowdLdapConfiguration {
}

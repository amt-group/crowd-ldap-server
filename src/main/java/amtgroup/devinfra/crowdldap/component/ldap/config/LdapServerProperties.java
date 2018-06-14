package amtgroup.devinfra.crowdldap.component.ldap.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * @author Vitaly Ogoltsov
 */
@ConfigurationProperties(prefix = "ldap.server")
@Data
public class LdapServerProperties {

    /**
     * Порт для подключения к серверу.
     */
    private int port = 10389;

    /**
     * Включить эмуляцию работы атрибута memberOf в Active Directory.
     */
    private boolean memberOfEmulateActiveDirectory = false;

}

package amtgroup.devinfra.crowdldap.component.crowd.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * @author Vitaly Ogoltsov
 */
@ConfigurationProperties("crowdldap")
@Data
public class CrowdLdapProperties {

    /**
     * LDAP-суффикс для crowd partition.
     */
    private String suffix = "dc=crowd";

    /**
     * Включить эмуляцию работы атрибута memberOf в Active Directory.
     */
    private boolean memberOfEmulateActiveDirectory = false;

    /**
     * Включать вложенные группы в memberOf.
     */
    private boolean memberOfIncludeNested = false;

    /**
     * Время жизни кеша.
     */
    private Integer cacheTtl = 60 * 60 * 1000;

}

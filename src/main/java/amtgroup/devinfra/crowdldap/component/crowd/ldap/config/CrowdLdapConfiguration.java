package amtgroup.devinfra.crowdldap.component.crowdldap.config;

import amtgroup.devinfra.crowdldap.component.crowdldap.CrowdAuthenticator;
import amtgroup.devinfra.crowdldap.component.crowdldap.CrowdPartition;
import com.atlassian.crowd.service.client.CrowdClient;
import org.apache.directory.server.core.DirectoryService;
import org.apache.directory.server.core.authn.Authenticator;
import org.apache.directory.server.core.partition.Partition;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author Vitaly Ogoltsov
 */
@Configuration
@EnableConfigurationProperties(CrowdLdapProperties.class)
public class CrowdLdapConfiguration {

    @Bean
    public Partition crowdPartition(CrowdLdapProperties crowdLdapProperties,
                                    CrowdClient crowdClient,
                                    DirectoryService directoryService) throws Exception {

        CrowdPartition partition = new CrowdPartition(
                crowdClient,
                crowdLdapProperties.isMemberOfEmulateActiveDirectory(),
                crowdLdapProperties.isMemberOfIncludeNested()
        );
        partition.setId("crowd");
        partition.setSuffix(crowdLdapProperties.getSuffix());
        partition.setSchemaManager(directoryService.getSchemaManager());
        partition.initialize();
        directoryService.addPartition(partition);
        return partition;
    }

    @Bean
    public Authenticator crowdAuthenticator(CrowdClient crowdClient) {
        return new CrowdAuthenticator(crowdClient);
    }

}

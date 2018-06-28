package amtgroup.devinfra.crowdldap.component.crowd.query;

import amtgroup.devinfra.crowdldap.component.crowd.exception.CrowdRemoteException;
import amtgroup.devinfra.crowdldap.component.crowdldap.config.CrowdLdapProperties;
import amtgroup.devinfra.crowdldap.component.crowdldap.exception.CrowdLdapException;
import amtgroup.devinfra.crowdldap.component.crowd.query.util.CrowdLdapConstants;
import com.atlassian.crowd.model.user.User;
import com.atlassian.crowd.search.query.entity.restriction.NullRestrictionImpl;
import com.atlassian.crowd.service.client.CrowdClient;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.directory.server.core.DirectoryService;
import org.apache.directory.shared.ldap.constants.SchemaConstants;
import org.apache.directory.shared.ldap.entry.DefaultServerEntry;
import org.apache.directory.shared.ldap.entry.ServerEntry;
import org.apache.directory.shared.ldap.exception.LdapInvalidDnException;
import org.apache.directory.shared.ldap.name.DN;
import org.apache.directory.shared.ldap.name.RDN;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.stream.Collectors;

/**
 * @author Vitaly Ogoltsov
 */
@Repository
@Slf4j
class CrowdLdapUserRepository {

    private final CrowdClient crowdClient;
    private final DirectoryService directoryService;

    private final DN usersDn;


    @Autowired
    public CrowdLdapUserRepository(CrowdLdapProperties crowdLdapProperties,
                                   CrowdClient crowdClient,
                                   DirectoryService directoryService) throws LdapInvalidDnException {

        this.crowdClient = crowdClient;
        this.directoryService = directoryService;
        this.usersDn = new DN(crowdLdapProperties.getSuffix()).add(CrowdLdapConstants.USERS_RDN);
    }


    @Cacheable(cacheNames = "crowd.users")
    public List<ServerEntry> findAll() {
        log.trace("findAll(): started");
        try {
            List<User> users = crowdClient.searchUsers(NullRestrictionImpl.INSTANCE, 0, Integer.MAX_VALUE);
            log.trace("findAll(): {} users found", users.size());
            return users.stream()
                    .map(this::createUserEntry)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            throw new CrowdRemoteException(e);
        }
    }

    public ServerEntry findOne(RDN rdn) {
        return findAll()
                .stream()
                .filter(user -> StringUtils.equalsIgnoreCase(user.getDn().getRdn().getName(), rdn.getName()))
                .findFirst()
                .orElse(null);
    }

    private ServerEntry createUserEntry(User user) {
        try {
            ServerEntry userEntry = new DefaultServerEntry(
                    directoryService.getSchemaManager(),
                    new DN().addAll(usersDn).add(new RDN(SchemaConstants.UID_AT, user.getName()))
            );
            userEntry.put(SchemaConstants.OBJECT_CLASS, SchemaConstants.INET_ORG_PERSON_OC);
            userEntry.put(SchemaConstants.OBJECT_CLASS_AT, SchemaConstants.TOP_OC, SchemaConstants.ORGANIZATIONAL_PERSON_OC, SchemaConstants.PERSON_OC, SchemaConstants.INET_ORG_PERSON_OC);
            userEntry.put(SchemaConstants.CN_AT, user.getDisplayName());
            userEntry.put(SchemaConstants.UID_AT, user.getName());
            userEntry.put("mail", user.getEmailAddress());
            userEntry.put("givenname", user.getFirstName());
            userEntry.put(SchemaConstants.SN_AT, user.getLastName());
            userEntry.put(SchemaConstants.OU_AT, "users");
            return userEntry;
        } catch (Exception e) {
            throw new CrowdLdapException(e);
        }
    }

    @Scheduled(fixedRate = 15000L)
    @CacheEvict(cacheNames = "crowd.users")
    public void flushCache() {
        log.debug("Очистка кеша.");
    }

}

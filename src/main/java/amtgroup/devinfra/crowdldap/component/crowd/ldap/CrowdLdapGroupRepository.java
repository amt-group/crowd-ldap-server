package amtgroup.devinfra.crowdldap.component.crowd.query;

import amtgroup.devinfra.crowdldap.component.crowd.exception.CrowdRemoteException;
import amtgroup.devinfra.crowdldap.component.crowdldap.config.CrowdLdapProperties;
import amtgroup.devinfra.crowdldap.component.crowdldap.exception.CrowdLdapException;
import amtgroup.devinfra.crowdldap.component.crowd.query.util.CrowdLdapConstants;
import com.atlassian.crowd.model.group.Group;
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
class CrowdLdapGroupRepository {

    private final CrowdClient crowdClient;
    private final DirectoryService directoryService;

    private final DN groupsDn;


    @Autowired
    public CrowdLdapGroupRepository(CrowdLdapProperties crowdLdapProperties,
                                    CrowdClient crowdClient,
                                    DirectoryService directoryService) throws LdapInvalidDnException {

        this.crowdClient = crowdClient;
        this.directoryService = directoryService;
        this.groupsDn = new DN(crowdLdapProperties.getSuffix()).add(CrowdLdapConstants.USERS_RDN);
    }


    @Cacheable(cacheNames = "crowd.groups")
    public List<ServerEntry> findAll() {
        log.trace("findAll(): started");
        try {
            List<Group> groups = crowdClient.searchGroups(NullRestrictionImpl.INSTANCE, 0, Integer.MAX_VALUE);
            log.trace("findAll(): {} groups found", groups.size());
            return groups.stream()
                    .map(this::createGroupEntry)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            throw new CrowdRemoteException(e);
        }
    }

    public ServerEntry findOne(RDN rdn) {
        return findAll()
                .stream()
                .filter(group -> StringUtils.equalsIgnoreCase(group.getDn().getRdn().getName(), rdn.getName()))
                .findFirst()
                .orElse(null);
    }

    private ServerEntry createGroupEntry(Group group) {
        try {
            ServerEntry groupEntry = new DefaultServerEntry(
                    directoryService.getSchemaManager(),
                    new DN().addAll(groupsDn).add(new RDN(SchemaConstants.CN_AT, group.getName()))
            );
            groupEntry.put(SchemaConstants.OBJECT_CLASS_AT, SchemaConstants.GROUP_OF_NAMES_OC);
            groupEntry.put(SchemaConstants.CN_AT, group.getName());
            groupEntry.put("description", group.getDescription());
            return groupEntry;
        } catch (Exception e) {
            throw new CrowdLdapException(e);
        }
    }

    @Scheduled(fixedRate = 15000L)
    @CacheEvict(cacheNames = "crowd.groups")
    public void flushCache() {
        log.debug("Очистка кеша.");
    }

}

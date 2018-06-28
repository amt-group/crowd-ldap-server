package amtgroup.devinfra.crowdldap.component.crowdldap;

import amtgroup.devinfra.crowdldap.component.crowd.config.CrowdLdapProperties;
import amtgroup.devinfra.crowdldap.component.crowd.exception.CrowdLdapException;
import amtgroup.devinfra.crowdldap.component.crowd.exception.CrowdRemoteException;
import amtgroup.devinfra.crowdldap.component.crowd.util.CrowdLdapConstants;
import com.atlassian.crowd.model.group.Group;
import com.atlassian.crowd.model.group.Membership;
import com.atlassian.crowd.search.query.entity.restriction.NullRestrictionImpl;
import com.atlassian.crowd.service.client.CrowdClient;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.directory.server.core.DirectoryService;
import org.apache.directory.shared.ldap.constants.SchemaConstants;
import org.apache.directory.shared.ldap.entry.DefaultServerEntry;
import org.apache.directory.shared.ldap.entry.ServerEntry;
import org.apache.directory.shared.ldap.exception.LdapException;
import org.apache.directory.shared.ldap.exception.LdapInvalidDnException;
import org.apache.directory.shared.ldap.name.DN;
import org.apache.directory.shared.ldap.name.RDN;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

/**
 * @author Vitaly Ogoltsov
 */
@Repository
@Slf4j
class CrowdLdapGroupRepository {

    private final CrowdClient crowdClient;
    private final DirectoryService directoryService;

    private final DN groupsDn;
    private final DN usersDn;


    @Autowired
    public CrowdLdapGroupRepository(CrowdLdapProperties crowdLdapProperties,
                                    CrowdClient crowdClient,
                                    DirectoryService directoryService) throws LdapInvalidDnException {

        this.crowdClient = crowdClient;
        this.directoryService = directoryService;
        this.groupsDn = new DN(crowdLdapProperties.getSuffix()).add(CrowdLdapConstants.USERS_RDN);
        this.usersDn = new DN(crowdLdapProperties.getSuffix()).add(CrowdLdapConstants.USERS_RDN);
    }


    @Cacheable(cacheNames = "crowd.groups")
    public List<ServerEntry> findAll() {
        log.trace("findAll(): started");
        try {
            List<Group> groups = crowdClient.searchGroups(NullRestrictionImpl.INSTANCE, 0, Integer.MAX_VALUE);
            log.trace("findAll(): {} groups found", groups.size());
            List<ServerEntry> groupEntries = groups.stream()
                    .map(this::createGroupEntry)
                    .collect(Collectors.toList());
            // load memberships
            log.trace("findAll(): loading memberships");
            Iterable<Membership> memberships = crowdClient.getMemberships();
            log.trace("findAll(): {} memberships loaded");
            groupEntries.forEach(this.addMembers(memberships));
            // return
            return groupEntries;
        } catch (Exception e) {
            throw new CrowdRemoteException(e);
        }
    }

    ServerEntry findOne(RDN rdn) {
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
                    new DN().addAll(groupsDn).add(new RDN(CrowdLdapConstants.GROUP_ID_AT, group.getName()))
            );
            groupEntry.put(SchemaConstants.OBJECT_CLASS_AT, SchemaConstants.GROUP_OF_NAMES_OC);
            groupEntry.put(CrowdLdapConstants.GROUP_ID_AT, group.getName());
            groupEntry.put("description", group.getDescription());
            return groupEntry;
        } catch (Exception e) {
            throw new CrowdLdapException(e);
        }
    }

    private Consumer<ServerEntry> addMembers(Iterable<Membership> memberships) {
        return groupEntry -> {
            StreamSupport.stream(memberships.spliterator(), false)
                    .filter(membership -> Objects.equals(membership.getGroupName(), groupEntry.getDn().getRdn().getNormValue()))
                    .forEach(membership -> {
                        addUserMembers(groupEntry, membership.getUserNames());
                        addGroupMembers(groupEntry, membership.getChildGroupNames());
                    });
        };
    }

    private void addUserMembers(ServerEntry groupEntry, Collection<String> userNames) {
        try {
            for (String userName : userNames) {
                groupEntry.add(SchemaConstants.MEMBER_AT, new DN().addAll(usersDn).add(new RDN(CrowdLdapConstants.USER_ID_AT, userName)).getName());
            }
        } catch (LdapException e) {
            throw new CrowdLdapException(e);
        }
    }

    private void addGroupMembers(ServerEntry groupEntry, Collection<String> userNames) {
        try {
            for (String userName : userNames) {
                groupEntry.add(SchemaConstants.MEMBER_AT, new DN().addAll(usersDn).add(new RDN(CrowdLdapConstants.GROUP_ID_AT, userName)).getName());
            }
        } catch (LdapException e) {
            throw new CrowdLdapException(e);
        }
    }

    @Scheduled(fixedRateString = "${crowdldap.cache-ttl:3600000}")
    @CacheEvict(cacheNames = "crowd.groups")
    public void flushCache() {
        log.debug("Очистка кеша.");
    }

}

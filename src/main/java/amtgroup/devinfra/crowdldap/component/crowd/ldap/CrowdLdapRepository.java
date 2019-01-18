package amtgroup.devinfra.crowdldap.component.crowd.ldap;

import amtgroup.devinfra.crowdldap.component.crowd.config.CrowdLdapProperties;
import amtgroup.devinfra.crowdldap.component.crowd.exception.CrowdLdapException;
import amtgroup.devinfra.crowdldap.component.crowd.exception.CrowdSyncException;
import amtgroup.devinfra.crowdldap.component.crowd.util.CrowdLdapConstants;
import com.atlassian.crowd.model.group.Group;
import com.atlassian.crowd.model.group.Membership;
import com.atlassian.crowd.model.user.User;
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
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * @author Vitaly Ogoltsov
 */
@Repository
@Slf4j
class CrowdLdapRepository {

    private final CrowdClient crowdClient;
    private final DirectoryService directoryService;

    private final DN groupsDn;
    private final DN usersDn;

    private volatile List<ServerEntry> groupEntries;
    private volatile List<ServerEntry> userEntries;


    @Autowired
    public CrowdLdapRepository(CrowdLdapProperties crowdLdapProperties,
                               CrowdClient crowdClient,
                               DirectoryService directoryService) throws LdapInvalidDnException {

        this.groupsDn = new DN(crowdLdapProperties.getSuffix()).add(CrowdLdapConstants.USERS_RDN);
        this.usersDn = new DN(crowdLdapProperties.getSuffix()).add(CrowdLdapConstants.USERS_RDN);
        this.crowdClient = crowdClient;
        this.directoryService = directoryService;
    }


    List<ServerEntry> findAllGroupEntries() {
        if (groupEntries == null) {
            throw new IllegalStateException();
        }
        return groupEntries;
    }

    Optional<ServerEntry> findGroupEntryById(RDN rdn) {
        return findAllGroupEntries()
                .stream()
                .filter(group -> StringUtils.equalsIgnoreCase(group.getDn().getRdn().getName(), rdn.getName()))
                .findFirst();
    }


    List<ServerEntry> findAllUserEntries() {
        if (userEntries == null) {
            throw new IllegalStateException();
        }
        return userEntries;
    }

    Optional<ServerEntry> findUserEntryById(RDN rdn) {
        return findAllUserEntries()
                .stream()
                .filter(user -> StringUtils.equalsIgnoreCase(user.getDn().getRdn().getName(), rdn.getName()))
                .findFirst();
    }


    @Scheduled(fixedRateString = "${crowdldap.cache-ttl:900000}", initialDelayString = "${crowdldap.cache-ttl:900000}")
    public void sync() {
        log.info("sync(): started");
        try {
            log.info("sync(): load all groups");
            Map<String, Group> groups = crowdClient.searchGroups(NullRestrictionImpl.INSTANCE, 0, Integer.MAX_VALUE)
                    .stream()
                    .collect(Collectors.toMap(Group::getName, Function.identity()));
            log.info("sync(): load all users");
            Map<String, User> users = crowdClient.searchUsers(NullRestrictionImpl.INSTANCE, 0, Integer.MAX_VALUE)
                    .stream()
                    .collect(Collectors.toMap(User::getName, Function.identity()));
            log.info("sync(): load memberships");
            Iterable<Membership> memberships = crowdClient.getMemberships();
            log.info("sync(): create group entries");
            Map<String, ServerEntry> groupEntries = groups
                    .entrySet()
                    .stream()
                    .collect(Collectors.toMap(Map.Entry::getKey, entry -> createGroupEntry(entry.getValue())));
            log.info("sync(): create user entries");
            Map<String, ServerEntry> userEntries = users
                    .entrySet()
                    .stream()
                    .collect(Collectors.toMap(Map.Entry::getKey, entry -> createUserEntry(entry.getValue())));
            log.info("sync(): map memberships");
            for (Membership membership : memberships) {
                String groupName = membership.getGroupName();
                Group group = groups.get(groupName);
                ServerEntry groupEntry = groupEntries.get(groupName);
                if (group == null || !group.isActive() || groupEntry == null) {
                    continue;
                }
                membership.getUserNames()
                        .stream()
                        .map(users::get)
                        .filter(User::isActive)
                        .map(user -> userEntries.get(user.getName()))
                        .forEach(userEntry -> {
                            addUserMemberOf(userEntry, groupEntry);
                            addGroupMember(groupEntry, userEntry);
                        });
                // todo: implement nested memberships
            }
            log.info("sync(): update cache");
            this.groupEntries = new ArrayList<>(groupEntries.values());
            this.userEntries = new ArrayList<>(userEntries.values());
            log.info("sync(): complete");
        } catch (Exception e) {
            log.error("sync(): failed", e);
            throw new CrowdSyncException(e);
        }
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

    private ServerEntry createUserEntry(User user) {
        try {
            ServerEntry userEntry = new DefaultServerEntry(
                    directoryService.getSchemaManager(),
                    new DN().addAll(usersDn).add(new RDN(CrowdLdapConstants.USER_ID_AT, user.getName()))
            );
            userEntry.put(SchemaConstants.OBJECT_CLASS, SchemaConstants.INET_ORG_PERSON_OC);
            userEntry.put(SchemaConstants.OBJECT_CLASS_AT, SchemaConstants.TOP_OC, SchemaConstants.ORGANIZATIONAL_PERSON_OC, SchemaConstants.PERSON_OC, SchemaConstants.INET_ORG_PERSON_OC);
            userEntry.put(SchemaConstants.CN_AT, user.getDisplayName());
            userEntry.put(CrowdLdapConstants.USER_ID_AT, user.getName());
            userEntry.put("mail", user.getEmailAddress());
            userEntry.put("givenname", user.getFirstName());
            userEntry.put(SchemaConstants.SN_AT, user.getLastName());
            userEntry.put(SchemaConstants.OU_AT, "users");
            return userEntry;
        } catch (Exception e) {
            throw new CrowdLdapException(e);
        }
    }

    private void addUserMemberOf(ServerEntry userEntry, ServerEntry groupEntry) {
        try {
            userEntry.add(CrowdLdapConstants.MEMBEROF_AT, groupEntry.getDn().getName());
        } catch (LdapException e) {
            e.printStackTrace();
        }
    }

    private void addGroupMember(ServerEntry groupEntry, ServerEntry userEntry) {
        try {
            groupEntry.add(CrowdLdapConstants.MEMBER_AT, userEntry.getDn().getName());
        } catch (LdapException e) {
            e.printStackTrace();
        }
    }

}

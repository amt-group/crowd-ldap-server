package amtgroup.devinfra.crowdldap.component.crowd.ldap;

import amtgroup.devinfra.crowdldap.component.crowd.config.CrowdLdapProperties;
import amtgroup.devinfra.crowdldap.component.crowd.exception.CrowdAuthenticationException;
import amtgroup.devinfra.crowdldap.component.crowd.exception.CrowdLdapException;
import amtgroup.devinfra.crowdldap.component.crowd.util.CrowdLdapConstants;
import com.atlassian.crowd.model.user.User;
import com.atlassian.crowd.service.client.CrowdClient;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.directory.server.core.LdapPrincipal;
import org.apache.directory.server.core.authn.AbstractAuthenticator;
import org.apache.directory.server.core.interceptor.context.BindOperationContext;
import org.apache.directory.shared.ldap.constants.AuthenticationLevel;
import org.apache.directory.shared.ldap.exception.LdapInvalidDnException;
import org.apache.directory.shared.ldap.name.DN;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Objects;

/**
 * Implements {@link org.apache.directory.server.core.authn.Authenticator}
 * to authenticate against Atlassian Crowd using a supplied {@link CrowdClient}.
 *
 * @author Dieter Wimberger (dieter at wimpi dot net)
 */
@Component
@Slf4j
public class CrowdAuthenticator extends AbstractAuthenticator {

    private final CrowdClient client;

    private final DN usersDn;


    @Autowired
    public CrowdAuthenticator(CrowdLdapProperties crowdLdapProperties,
                              CrowdClient client) throws LdapInvalidDnException {

        super("simple");
        this.client = Objects.requireNonNull(client);
        this.usersDn = new DN(crowdLdapProperties.getSuffix()).add(CrowdLdapConstants.USERS_RDN);
    }


    @Override
    public LdapPrincipal authenticate(BindOperationContext ctx) {
        log.trace("authenticate(): {}", ctx);
        try {
            DN bindDn = ctx.getDn();
            if (!StringUtils.endsWithIgnoreCase(bindDn.getName(), this.usersDn.getName())
                    || bindDn.size() != this.usersDn.size() + 1) {

                log.error("authenticate() => invalid bind DN: {}", ctx);
            }
            String user = ctx.getDn().getSuffix(this.usersDn.size()).getRdn().getNormValue();
            String pass = new String(ctx.getCredentials(), "utf-8");
            User u = client.authenticateUser(user, pass);
            if (u == null) {
                log.warn("authenticate() => failed: {}", ctx);
                throw new CrowdAuthenticationException(user);
            } else {
                log.trace("authenticate() => success: {}", ctx);
                return new LdapPrincipal(ctx.getDn(), AuthenticationLevel.SIMPLE);
            }
        } catch (Exception ex) {
            log.error("authenticate() => error:", ctx, ex);
            throw new CrowdLdapException(ex);
        }
    }

}

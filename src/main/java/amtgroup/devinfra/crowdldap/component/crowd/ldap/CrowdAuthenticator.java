package amtgroup.devinfra.crowdldap.component.crowdldap;

import amtgroup.devinfra.crowdldap.component.crowdldap.exception.CrowdLdapAuthenticationException;
import amtgroup.devinfra.crowdldap.component.crowdldap.exception.CrowdLdapException;
import com.atlassian.crowd.model.user.User;
import com.atlassian.crowd.service.client.CrowdClient;
import lombok.extern.slf4j.Slf4j;
import org.apache.directory.server.core.LdapPrincipal;
import org.apache.directory.server.core.authn.AbstractAuthenticator;
import org.apache.directory.server.core.interceptor.context.BindOperationContext;
import org.apache.directory.shared.ldap.constants.AuthenticationLevel;

import java.util.Objects;

/**
 * Implements {@link org.apache.directory.server.core.authn.Authenticator}
 * to authenticate against Atlassian Crowd using a supplied {@link CrowdClient}.
 *
 * @author Dieter Wimberger (dieter at wimpi dot net)
 */
@Slf4j
public class CrowdAuthenticator extends AbstractAuthenticator {

    private final CrowdClient client;


    public CrowdAuthenticator(CrowdClient client) {
        super("simple");
        this.client = Objects.requireNonNull(client);
    }


    @Override
    public LdapPrincipal authenticate(BindOperationContext ctx) throws Exception {
        String user = ctx.getDn().getRdn(2).getNormValue();
        String pass = new String(ctx.getCredentials(), "utf-8");

        try {
            User u = client.authenticateUser(user, pass);
            if (u == null) {
                throw new CrowdLdapAuthenticationException(user);
            } else {
                return new LdapPrincipal(ctx.getDn(), AuthenticationLevel.SIMPLE);
            }
        } catch (Exception ex) {
            throw new CrowdLdapException(ex);
        }
    }

}

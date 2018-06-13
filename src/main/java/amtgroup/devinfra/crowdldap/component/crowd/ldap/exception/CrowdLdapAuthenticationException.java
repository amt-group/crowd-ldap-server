package amtgroup.devinfra.crowdldap.component.crowdldap.exception;

import amtgroup.devinfra.crowdldap.util.exception.ApplicationException;

/**
 * @author Vitaly Ogoltsov
 */
public class CrowdLdapAuthenticationException extends ApplicationException {

    public CrowdLdapAuthenticationException(String username) {
        super(username);
    }

}

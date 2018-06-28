package amtgroup.devinfra.crowdldap.component.crowdldap.exception;

import amtgroup.devinfra.crowdldap.util.exception.ApplicationException;

/**
 * @author Vitaly Ogoltsov
 */
public class CrowdAuthenticationException extends ApplicationException {

    public CrowdAuthenticationException(String username) {
        super(username);
    }

}

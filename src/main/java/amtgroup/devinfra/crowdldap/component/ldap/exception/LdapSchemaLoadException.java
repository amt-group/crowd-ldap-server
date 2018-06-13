package amtgroup.devinfra.crowdldap.component.ldap.exception;

import amtgroup.devinfra.crowdldap.util.exception.ApplicationException;

/**
 * @author Vitaly Ogoltsov
 */
public class LdapSchemaLoadException extends ApplicationException {

    public LdapSchemaLoadException(Throwable cause) {
        super(cause);
    }

}

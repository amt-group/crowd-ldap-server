package amtgroup.devinfra.crowdldap.component.crowd.exception;

import amtgroup.devinfra.crowdldap.util.exception.ApplicationException;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * @author Vitaly Ogoltsov
 */
@ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
public class CrowdRemoteException extends ApplicationException {

    public CrowdRemoteException(Throwable cause) {
        super(cause);
    }

}

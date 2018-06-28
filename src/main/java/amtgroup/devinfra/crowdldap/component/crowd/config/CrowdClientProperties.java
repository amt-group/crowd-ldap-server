package amtgroup.devinfra.crowdldap.component.crowd.client.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * @author Vitaly Ogoltsov
 */
@ConfigurationProperties(prefix = "crowd.client")
@Data
public class CrowdClientProperties {

    private String test;

    /**
     * Настройки подключения к Crowd.
     */
    private Server server;

    /**
     * Настройки идентификации текущего приложения в Crowd.
     */
    private Application application;

    /**
     * Настройки сессии подключения к Crowd.
     */
    private Session session = new Session();


    @Data
    public static class Server {

        private String url;

    }

    @Data
    public static class Application {

        private String name;

        private String password;

        private String loginUrl;

    }

    @Data
    public static class Session {

        private long validationInterval = 0;

        private String lastValidation = "session.lastvalidation";

        private String isAuthenticated = "session.isauthenticated";

        private String tokenKey = "session.tokenkey";

    }

}

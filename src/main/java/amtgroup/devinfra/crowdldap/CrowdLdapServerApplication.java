package amtgroup.devinfra.crowdldap;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.AdviceMode;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableCaching(mode = AdviceMode.ASPECTJ, proxyTargetClass = true)
@EnableScheduling
public class CrowdLdapServerApplication {

    public static void main(String[] args) {
        SpringApplication.run(CrowdLdapServerApplication.class, args);
    }

}

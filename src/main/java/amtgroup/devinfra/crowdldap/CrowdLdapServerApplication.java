package amtgroup.devinfra.crowdldap;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class CrowdLdapServerApplication {

    public static void main(String[] args) {
        SpringApplication.run(CrowdLdapServerApplication.class, args);
    }

}

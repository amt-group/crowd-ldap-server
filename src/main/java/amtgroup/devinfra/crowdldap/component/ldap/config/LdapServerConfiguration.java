package amtgroup.devinfra.crowdldap.component.ldap.config;

import amtgroup.devinfra.crowdldap.component.ldap.exception.LdapSchemaLoadException;
import amtgroup.devinfra.crowdldap.component.ldap.util.SpringSchemaLdifExtractor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.directory.server.constants.ServerDNConstants;
import org.apache.directory.server.core.DefaultDirectoryService;
import org.apache.directory.server.core.DirectoryService;
import org.apache.directory.server.core.authn.AuthenticationInterceptor;
import org.apache.directory.server.core.authn.Authenticator;
import org.apache.directory.server.core.partition.impl.btree.jdbm.JdbmPartition;
import org.apache.directory.server.core.partition.ldif.LdifPartition;
import org.apache.directory.server.core.schema.SchemaPartition;
import org.apache.directory.server.ldap.LdapServer;
import org.apache.directory.server.protocol.shared.transport.TcpTransport;
import org.apache.directory.shared.ldap.schema.SchemaManager;
import org.apache.directory.shared.ldap.schema.ldif.extractor.SchemaLdifExtractor;
import org.apache.directory.shared.ldap.schema.loader.ldif.LdifSchemaLoader;
import org.apache.directory.shared.ldap.schema.manager.impl.DefaultSchemaManager;
import org.apache.directory.shared.ldap.schema.registries.SchemaLoader;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;
import java.util.Set;

/**
 * @author Vitaly Ogoltsov
 */
@Configuration
@EnableConfigurationProperties(LdapServerProperties.class)
@Slf4j
public class LdapServerConfiguration {

    private File workingDirectory;


    private File getWorkingDirectory() {
        if (workingDirectory == null) {
            String workingDirectoryString = System.getProperty("ldap.server.work.dir");
            if (StringUtils.isNotBlank(workingDirectoryString)) {
                workingDirectory = new File(workingDirectoryString);
            } else {
                workingDirectory = new File("work/ldap-server");
            }
            workingDirectory = workingDirectory.getAbsoluteFile();
            if (!workingDirectory.exists() && !workingDirectory.mkdirs()) {
                throw new RuntimeException("Ошибка создания рабочей директории ldap-сервера: " + workingDirectory.getAbsolutePath());
            }
        }
        return workingDirectory;
    }


    @Bean
    public SchemaManager schemaManager(LdapServerProperties ldapServerProperties) throws Exception {

        // извлечь файлы схем
        SchemaLdifExtractor extractor = new SpringSchemaLdifExtractor(getWorkingDirectory());
        extractor.extractOrCopy(true);
        // настроить работу атрибута memberOf
        File attributeTypesDir = new File(getWorkingDirectory(), "schema/ou=schema/cn=other/ou=attributetypes");
        if (!attributeTypesDir.exists()) {
            if (!attributeTypesDir.mkdirs()) {
                throw new RuntimeException("Ошибка создания директории типов атрибутов: " + attributeTypesDir.getAbsolutePath());
            }
            File memberOfLDIF = new File(attributeTypesDir, "m-oid=1.2.840.113556.1.2.102.ldif");
            if (!memberOfLDIF.exists()) {
                InputStream in = null;
                OutputStream out = null;
                try {
                    in = getClass().getClassLoader().getResourceAsStream("ldap/memberOf.ldif");
                    out = new FileOutputStream(memberOfLDIF);
                    IOUtils.copy(in, out);
                } finally {
                    IOUtils.closeQuietly(in);
                    IOUtils.closeQuietly(out);
                }
            }
        }
        // инициализировать менеджер схем
        SchemaLoader loader = new LdifSchemaLoader(new File(getWorkingDirectory(), "schema"));
        SchemaManager schemaManager = new DefaultSchemaManager(loader);
        // необходимо загрузить все схемы сейча, иначе возникнут ошибки при загрузке других партиций
        schemaManager.loadAllEnabled();
        // проверить наличие ошибок загрузки схемы
        List<Throwable> errors = schemaManager.getErrors();
        if (errors.size() != 0) {
            for (Throwable cause : errors) {
                log.error("Ошибка загрузки LDAP-схемы", cause);
            }
            throw new LdapSchemaLoadException(errors.get(0));
        }
        // вернуть менеджер схем
        return schemaManager;
    }

    @Bean
    public SchemaPartition schemaPartition(SchemaManager schemaManager) {
        LdifPartition ldifPartition = new LdifPartition();
        ldifPartition.setWorkingDirectory(new File(getWorkingDirectory(), "schema").getAbsolutePath());
        SchemaPartition schemaPartition = new SchemaPartition();
        schemaPartition.setWrappedPartition(ldifPartition);
        schemaPartition.setSchemaManager(schemaManager);
        return schemaPartition;
    }

    @Bean
    public DirectoryService directoryService(SchemaManager schemaManager,
                                             SchemaPartition schemaPartition,
                                             Set<Authenticator> authenticators) throws Exception {

        DirectoryService directoryService = new DefaultDirectoryService();
        // установить рабочую директорию сервиса
        directoryService.setWorkingDirectory(getWorkingDirectory());
        directoryService.setSchemaManager(schemaManager);
        directoryService.getSchemaService().setSchemaPartition(schemaPartition);
        // настроить system partition
        JdbmPartition systemPartition = new JdbmPartition();
        systemPartition.setId("system");
        systemPartition.setPartitionDir(new File(directoryService.getWorkingDirectory(), systemPartition.getId()));
        systemPartition.setSuffix(ServerDNConstants.SYSTEM_DN);
        directoryService.addPartition(systemPartition);
        directoryService.setSystemPartition(systemPartition);
        // настроить сервис
        directoryService.getChangeLog().setEnabled(false);
        directoryService.setDenormalizeOpAttrsEnabled(true);
        // запретить анонимный доступ
        directoryService.setAllowAnonymousAccess(false);
        // настроить способы аутентификации
        ((AuthenticationInterceptor) directoryService.getInterceptor(AuthenticationInterceptor.class.getName()))
                .setAuthenticators(authenticators);
        // запустить сервиис
        directoryService.startup();
        return directoryService;
    }

    @Bean
    public LdapServer ldapServer(LdapServerProperties ldapServerProperties,
                                 DirectoryService directoryService) throws Exception {

        // настроить и запустить LdapServer
        LdapServer ldapServer = new LdapServer();
        ldapServer.setTransports(new TcpTransport(ldapServerProperties.getPort()));
        ldapServer.setDirectoryService(directoryService);
        ldapServer.start();
        return ldapServer;
    }


}

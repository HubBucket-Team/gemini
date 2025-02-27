package it.at7.gemini.auth.core;

import it.at7.gemini.conf.State;
import it.at7.gemini.core.Module;
import it.at7.gemini.core.*;
import it.at7.gemini.core.persistence.PersistenceEntityManager;
import it.at7.gemini.exceptions.GeminiException;
import it.at7.gemini.schema.Entity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.stereotype.Service;

import java.util.Optional;

import static it.at7.gemini.conf.State.SCHEMA_STORAGE_INITIALIZED;

@Service
@ModuleDescription(
        name = "AUTH",
        dependencies = {"CORE"},
        order = -607)
@ComponentScan("it.at7.gemini.auth.core")
@ConditionalOnProperty(name = "gemini.auth", matchIfMissing = true)
public class AuthModule implements Module {
    private static final Logger logger = LoggerFactory.getLogger(AuthModule.class);

    private final SchemaManager schemaManager;
    private final TransactionManager transactionManager;
    private final PersistenceEntityManager persistenceEntityManager;
    private final ApplicationContext applicationContext;

    @Autowired
    public AuthModule(SchemaManager schemaManager,
                      TransactionManager transactionManager,
                      PersistenceEntityManager persistenceEntityManager,
                      ApplicationContext applicationContext
    ) {
        this.schemaManager = schemaManager;
        this.transactionManager = transactionManager;
        this.persistenceEntityManager = persistenceEntityManager;
        this.applicationContext = applicationContext;
    }

    @Override
    public void onChange(State previous, State actual, Optional<Transaction> transaction) throws GeminiException {
        if (actual == SCHEMA_STORAGE_INITIALIZED) {
            checkOrcreatePredefinedUsers(transaction);
        }
    }

    private void checkOrcreatePredefinedUsers(Optional<Transaction> transaction) throws GeminiException {
        logger.info("Check/Create predefined Users");
        assert transaction.isPresent();
        Transaction t = transaction.get();
        Entity userEntity = schemaManager.getEntity(UserRef.NAME);

        // GEMINI Core User
        String username = AuthModuleRef.USERS.GEMINI;
        EntityReferenceRecord entityReferenceRecord = FieldConverters.logicalKeyFromObject(userEntity, username);
        Optional<EntityRecord> userRec = persistenceEntityManager.getEntityRecordByLogicalKey(userEntity, entityReferenceRecord, t);
        if (!userRec.isPresent()) {
            String description = "Auto generated user for " + username;
            EntityRecord geminiFrameworkUser = new EntityRecord(userEntity);
            geminiFrameworkUser.put(UserRef.FIELDS.USERNAME, username);
            geminiFrameworkUser.put(UserRef.FIELDS.DESCRIPTION, description);
            geminiFrameworkUser.put(UserRef.FIELDS.FRAMEWORK, true);
            persistenceEntityManager.createOrUpdateEntityRecord(geminiFrameworkUser, t);
        }

        // Admin
        String adminUsername = AuthModuleRef.USERS.ADMINISTRATOR;
        entityReferenceRecord = FieldConverters.logicalKeyFromObject(userEntity, adminUsername);
        Optional<EntityRecord> adminRec = persistenceEntityManager.getEntityRecordByLogicalKey(userEntity, entityReferenceRecord, t);
        if (!adminRec.isPresent()) {
            String adminiDescription = "Auto generated user for " + adminUsername;
            EntityRecord adminUer = new EntityRecord(userEntity);
            adminUer.put(UserRef.FIELDS.USERNAME, adminUsername);
            adminUer.put(UserRef.FIELDS.DESCRIPTION, adminiDescription);
            adminUer.put(UserRef.FIELDS.FRAMEWORK, false);
            adminUer.put(UserRef.FIELDS.PASSWORD, adminUsername);
            persistenceEntityManager.createOrUpdateEntityRecord(adminUer, t);
        }
    }

}

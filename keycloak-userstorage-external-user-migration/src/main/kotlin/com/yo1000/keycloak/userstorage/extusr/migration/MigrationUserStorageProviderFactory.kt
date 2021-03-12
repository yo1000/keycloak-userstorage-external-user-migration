package com.yo1000.keycloak.userstorage.extusr.migration

import org.apache.http.impl.client.HttpClientBuilder
import org.keycloak.component.ComponentModel
import org.keycloak.models.KeycloakSession
import org.keycloak.storage.UserStorageProviderFactory

class MigrationUserStorageProviderFactory : UserStorageProviderFactory<MigrationUserStorageProvider> {
    companion object {
        const val ID = "external-user-migration"
    }

    override fun getId(): String = ID

    override fun create(keycloakSession: KeycloakSession, component: ComponentModel): MigrationUserStorageProvider {
        return MigrationUserStorageProvider(keycloakSession, component, HttpClientBuilder
                .create()
                .build()
        )
    }
}

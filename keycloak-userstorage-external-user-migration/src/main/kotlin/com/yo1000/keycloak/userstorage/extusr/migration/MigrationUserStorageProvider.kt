package com.yo1000.keycloak.userstorage.extusr.migration

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import org.apache.http.client.HttpClient
import org.apache.http.client.methods.HttpGet
import org.apache.http.util.EntityUtils
import org.keycloak.common.util.Time
import org.keycloak.component.ComponentModel
import org.keycloak.credential.*
import org.keycloak.models.*
import org.keycloak.storage.UserStorageProvider

abstract class AbstractMigrationUserProvider(
        val keycloakSession: KeycloakSession
) : UserStorageProvider, UserProvider by keycloakSession.userLocalStorage() {
    override fun searchForUser(search: String?, realm: RealmModel?): MutableList<UserModel> {
        return mutableListOf()
    }

    override fun searchForUser(search: String?, realm: RealmModel?, firstResult: Int, maxResults: Int): MutableList<UserModel> {
        return mutableListOf()
    }

    override fun searchForUser(params: MutableMap<String, String>?, realm: RealmModel?): MutableList<UserModel> {
        return mutableListOf()
    }

    override fun searchForUser(params: MutableMap<String, String>?, realm: RealmModel?, firstResult: Int, maxResults: Int): MutableList<UserModel> {
        return mutableListOf()
    }
}

class MigrationUserStorageProvider(
        keycloakSession: KeycloakSession,
        private val componentModel: ComponentModel,
        private val httpClient: HttpClient
) : AbstractMigrationUserProvider(keycloakSession), CredentialInputValidator {
    companion object {
        const val BASE_URL: String = "http://apistub:8080"
        const val AUTHORIZATION_PHRASE: String = "SECRET"
    }

    override fun close() {}

    override fun getUserByEmail(email: String, realm: RealmModel): UserModel? {
         return findAndCreateUserModelByEmail(email, realm)
                 ?: super.getUserByEmail(email, realm)
    }

    override fun getUserByUsername(username: String, realm: RealmModel): UserModel? {
        return findAndCreateUserModelByUsername(username, realm)
                ?: super.getUserByUsername(username, realm)
    }

     /**
      * @param storageId ID format is follows. "f:${componentId}:${externalId}"
      * https://www.keycloak.org/docs/latest/server_development/#storage-ids
      */
    override fun getUserById(storageId: String, realm: RealmModel): UserModel? {
        val storageIdParts: List<String> = Regex("f:([^:]+):(.+)").find(storageId)?.groupValues
                ?: throw IllegalArgumentException("storageId is illegal format")

        // val componentId: String = storageIdParts[1] // useless
        val username: String = storageIdParts[2]

        return findAndCreateUserModelByUsername(username, realm)
                ?: super.getUserById(storageId, realm)
    }

     override fun supportsCredentialType(credentialType: String): Boolean {
         return credentialType == CredentialModel.PASSWORD
     }

    override fun isConfiguredFor(realm: RealmModel, user: UserModel, credentialType: String): Boolean {
        if (!supportsCredentialType(credentialType))
            return false

        if (getPasswordCredentialProvider(keycloakSession).isConfiguredFor(realm, user, credentialType))
            return true

        val foundUser: User = findUserFromUrl("$BASE_URL/users/search?username=${user.username}") ?: return false
        return foundUser.password.isNotEmpty()
    }

    override fun isValid(realm: RealmModel, user: UserModel, credential: CredentialInput): Boolean {
        if (!supportsCredentialType(credential.type))
            return false

        if (credential !is UserCredentialModel)
            return false

        val rawPassword: String = credential.value
                ?: return false

        val foundUser: User = findUserFromUrl("$BASE_URL/users/search?username=${user.username}")
                ?: return false

        if (rawPassword != foundUser.password)
            return false

        getPasswordCredentialProvider(keycloakSession).createCredential(realm, user, rawPassword)
        keycloakSession.userLocalStorage().getUserById(user.id, realm).federationLink = null
        return true
    }

     private fun getPasswordCredentialProvider(keycloakSession: KeycloakSession): PasswordCredentialProvider {
         return keycloakSession.getProvider(
                 CredentialProvider::class.java,
                 PasswordCredentialProviderFactory.PROVIDER_ID
         ) as PasswordCredentialProvider
     }

     private fun findAndCreateUserModelByUsername(username: String, realm: RealmModel): UserModel? {
         return keycloakSession.userLocalStorage().getUserByUsername(username, realm)
                 ?: findUserFromUrl("$BASE_URL/users/search?username=$username")?.let { resp ->
                     keycloakSession.userLocalStorage().addUser(realm, resp.username)?.also { user ->
                         user.username = resp.username
                         user.email = resp.email
                         user.firstName = resp.firstName
                         user.lastName = resp.lastName
                         user.isEnabled = true
                         user.isEmailVerified = true
                         user.createdTimestamp = Time.currentTimeMillis()

                         user.federationLink = componentModel.id
                     }
                 }
     }

     private fun findAndCreateUserModelByEmail(email: String, realm: RealmModel): UserModel? {
         return keycloakSession.userLocalStorage().getUserByEmail(email, realm)
                 ?: findUserFromUrl("$BASE_URL/users/search?email=$email")?.let { resp ->
                     keycloakSession.userLocalStorage().addUser(realm, resp.username)?.also { user ->
                         user.username = resp.username
                         user.email = resp.email
                         user.firstName = resp.firstName
                         user.lastName = resp.lastName
                         user.isEnabled = true
                         user.isEmailVerified = true
                         user.createdTimestamp = Time.currentTimeMillis()

                         user.federationLink = componentModel.id
                     }
                 }
     }

     private fun findUserFromUrl(url: String): User? {
        return httpClient.execute(HttpGet(url).also {
            it.addHeader("Authorization", AUTHORIZATION_PHRASE)
        }).let {
            EntityUtils.toString(it.entity)
        }.let {
            ObjectMapper().readValue(it, object: TypeReference<ArrayList<User>>() {}).takeIf {
                it.isNotEmpty()
            }?.first()
        }
    }
}

data class User @JsonCreator constructor(
        @JsonProperty("id")
        val id: String,
        @JsonProperty("username")
        val username: String,
        @JsonProperty("firstName")
        val firstName: String,
        @JsonProperty("lastName")
        val lastName: String,
        @JsonProperty("email")
        val email: String,
        @JsonProperty("password")
        val password: String
)

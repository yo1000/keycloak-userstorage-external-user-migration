package com.yo1000.stub

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.*

@SpringBootApplication
class StubApplication

fun main(args: Array<String>) {
    runApplication<StubApplication>(*args)
}

@RestController
@RequestMapping("/users")
class StubUserController {
    companion object {
        const val AUTH_SECRET = "SECRET"
        val userStore: UserStoreMap = UserStoreMap(mutableMapOf(
                "RRRRRRRR-RRRR-4RRR-rRRR-aaaaaaaaaaaa".let {
                    it to User(
                            id = it,
                            username = "aaaa",
                            email = "aaaa@localhost",
                            firstName = "aa",
                            lastName = "AA",
                            password = "aaaa1111"
                    )
                },
                "RRRRRRRR-RRRR-4RRR-rRRR-bbbbbbbbbbbb".let {
                    it to User(
                            id = it,
                            username = "bbbb",
                            email = "bbbb@localhost",
                            firstName = "bb",
                            lastName = "BB",
                            password = "bbbb2222"
                    )
                },
                "RRRRRRRR-RRRR-4RRR-rRRR-cccccccccccc".let {
                    it to User(
                            id = it,
                            username = "cccc",
                            email = "cccc@localhost",
                            firstName = "cc",
                            lastName = "CC",
                            password = "cccc3333"
                    )
                },
                "RRRRRRRR-RRRR-4RRR-rRRR-dddddddddddd".let {
                    it to User(
                            id = it,
                            username = "dddd",
                            email = "dddd@localhost",
                            firstName = "dd",
                            lastName = "DD",
                            password = "dddd4444"
                    )
                }
        ))
    }

    @GetMapping("")
    @ResponseStatus(HttpStatus.OK)
    fun get(): List<User> {
        return userStore.values.toList()
    }

    @GetMapping("/{id}")
    @ResponseStatus(HttpStatus.OK)
    fun getById(@PathVariable id: String): User {
        return userStore[id]
                ?: throw ResourceNotFoundException(id)
    }

    @GetMapping("/search")
    @ResponseStatus(HttpStatus.OK)
    fun doSearch(
            @RequestParam(name = "username", required = false) username: String?,
            @RequestParam(name = "email", required = false) email: String?
    ): List<User> {
        return userStore.values.filter {
            it.username == username || it.email == email
        }
    }

    @PostMapping("")
    @ResponseStatus(HttpStatus.CREATED)
    fun post(@RequestBody user: User) {
        if (user.id in userStore.keys)
            throw ResourceDuplicatedException(user.id)

        if (userStore.duplicatesEmail(user))
            throw ResourceDuplicatedException(user.email)

        userStore[user.id] = user
    }

    @PutMapping("/{id}")
    @ResponseStatus(HttpStatus.OK)
    fun putById(@PathVariable("id") id: String, @RequestBody user: User) {
        if (id != user.id)
            throw IllegalArgumentException("$id != ${user.id}")

        if (id !in userStore.keys)
            throw ResourceNotFoundException(id)

        if (userStore.duplicatesEmail(user))
            throw ResourceDuplicatedException(user.email)

        userStore[user.id] = user
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.OK)
    fun deleteById(@PathVariable id: String): User {
        return userStore.remove(id)
                ?: throw ResourceNotFoundException(id)
    }

    @ModelAttribute
    fun checkAuthHeader(@RequestHeader("Authorization") authorization: String) {
        if (authorization != AUTH_SECRET)
            throw UnauthorizedException("Not allowed")
    }

    @ExceptionHandler(UnauthorizedException::class)
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    fun respUnauthorized() {}

    @ExceptionHandler(IllegalArgumentException::class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    fun respBadRequest() {}

    @ExceptionHandler(ResourceNotFoundException::class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    fun respNotFound() {}

    @ExceptionHandler(ResourceDuplicatedException::class)
    @ResponseStatus(HttpStatus.CONFLICT)
    fun respConflict() {}

    class UserStoreMap(initialMap: MutableMap<String, User>) : MutableMap<String, User> by initialMap {
        fun duplicatesEmail(user: User): Boolean {
            return any { user.id != it.value.id && user.email == it.value.email }
        }
    }

    data class User(
            val id: String,
            val username: String,
            val firstName: String,
            val lastName: String,
            val email: String,
            val password: String
    )

    class ResourceDuplicatedException(m: String) : IllegalArgumentException(m)
    class ResourceNotFoundException(m: String) : IllegalArgumentException(m)
    class UnauthorizedException(m: String): IllegalArgumentException(m)
}
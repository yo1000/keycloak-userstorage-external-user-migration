# keycloak-userstorage-external-user-migration
Keycloak UserStorage for External user migration

## How to build
```
./mvnw clean install
```

## How to Run for local dev
```
./mvnw clean install && docker-compose up --build
```

### Configure User Federation

1. Move to http://localhost:8080/auth/admin/
    1. Sign In by `admin` user
2. Click [User Federation] link
    1. Chose `external-user-migration` in [Add provider...] select box
    2. Click [Save] button
    3. Click [Sign Out] button in Right top corner menu

### Migrate user from external system

1. Move to http://localhost:8080/auth/realms/master/account/#/personal-info
    1. Sign In by `alice` user
    2. Click [Sign Out] button
1. Move to http://localhost:8080/auth/admin/
    1. Sign In by `admin` user
2. Click [Users] link
    1. Click [View all users] button

### Demo users

in Keycloak

| Username | Email | Password |
|---|---|---|
| `admin` | | `admin1234` |

in External system

| Username | Email | Password |
|---|---|---|
| `alice`   | `alice@localhost`   | `alice1234`   |
| `bob`     | `bob@localhost`     | `bob1234`     |
| `charlie` | `charlie@localhost` | `charlie1234` |
| `dave`    | `dave@localhost`    | `dave1234`    |

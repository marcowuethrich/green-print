package ch.wuethrich.greenprint.auth

import ch.wuethrich.greenprint.entity.UserEntity
import jakarta.ws.rs.WebApplicationException
import jakarta.ws.rs.core.Response

fun requireOwner(entityUser: UserEntity, currentUser: UserEntity) {
    if (entityUser.id != currentUser.id) throw WebApplicationException(Response.Status.FORBIDDEN)
}

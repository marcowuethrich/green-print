package ch.wuethrich.greenprint.resource

import jakarta.persistence.PersistenceException
import jakarta.ws.rs.core.Response
import jakarta.ws.rs.ext.ExceptionMapper
import jakarta.ws.rs.ext.Provider
import org.hibernate.exception.ConstraintViolationException

@Provider
class PersistenceExceptionMapper : ExceptionMapper<PersistenceException> {
    override fun toResponse(e: PersistenceException): Response {
        val isConstraintViolation = generateSequence<Throwable>(e) { it.cause }
            .any { it is ConstraintViolationException }
        return if (isConstraintViolation)
            Response.status(Response.Status.CONFLICT).build()
        else
            Response.status(Response.Status.INTERNAL_SERVER_ERROR).build()
    }
}

@Provider
class ValidationExceptionMapper : ExceptionMapper<ValidationException> {
    override fun toResponse(e: ValidationException): Response =
        Response.status(Response.Status.BAD_REQUEST).entity(e.error).build()
}

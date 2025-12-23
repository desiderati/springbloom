/*
 * Copyright (c) 2025 - Felipe Desiderati
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and
 * associated documentation files (the "Software"), to deal in the Software without restriction,
 * including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense,
 * and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so,
 * subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial
 * portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT
 * LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.
 * IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
 * SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
package dev.springbloom.web.graphql.exception

import dev.springbloom.core.exception.ApplicationException
import dev.springbloom.core.validation.TypedValidationException
import graphql.GraphQLError
import graphql.GraphqlErrorBuilder
import graphql.execution.DataFetcherExceptionHandlerParameters
import graphql.schema.DataFetchingEnvironment
import org.hibernate.exception.ConstraintViolationException
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.condition.ConditionalOnSingleCandidate
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication
import org.springframework.context.MessageSource
import org.springframework.context.i18n.LocaleContextHolder
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.graphql.data.method.annotation.GraphQlExceptionHandler
import org.springframework.graphql.execution.ErrorType
import org.springframework.stereotype.Controller
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.ControllerAdvice
import java.lang.reflect.UndeclaredThrowableException
import java.util.*

private const val DEFAULT_CONSTRAINT_VIOLATION_ERROR_MESSAGE = "constraint_violation_exception"

@Validated
@ConditionalOnWebApplication
@ConditionalOnSingleCandidate(GraphQLExceptionHandlerController::class)
@ControllerAdvice(annotations = [Controller::class])
class GraphQLExceptionHandlerController(private val messageSource: MessageSource) {

    companion object {
        private val logNotSafe: Logger =
            LoggerFactory.getLogger(
                String.format("%sNotPrivacySafe", GraphQLExceptionHandlerController::class.java.getName())
            )

        private const val DEFAULT_ERROR_CODE = "unexpected_condition_exception"
        private const val DEFAULT_ERROR_MESSAGE =
            "The server encountered an unexpected condition that prevented it from fulfilling the request!"
    }

    @GraphQlExceptionHandler(UndeclaredThrowableException::class)
    fun handleException(
        undeclaredThrowableException: UndeclaredThrowableException,
        environment: DataFetchingEnvironment
    ): GraphQLError {
        return generateGraphQLError(
            undeclaredThrowableException,
            unwrapException(undeclaredThrowableException).message,
            ErrorType.BAD_REQUEST,
            environment
        )
    }

    @GraphQlExceptionHandler(Throwable::class)
    fun handleException(throwable: Throwable, environment: DataFetchingEnvironment): GraphQLError {
        return generateGraphQLError(
            throwable,
            unwrapException(throwable).message,
            ErrorType.BAD_REQUEST,
            environment
        )
    }

    @GraphQlExceptionHandler(ApplicationException::class)
    fun handleException(
        applicationException: ApplicationException,
        environment: DataFetchingEnvironment
    ): GraphQLError {
        return generateGraphQLError(
            applicationException,
            unwrapException(applicationException).message,
            ErrorType.BAD_REQUEST,
            environment
        )
    }

    @GraphQlExceptionHandler(TypedValidationException::class)
    fun handleException(
        typedValidationException: TypedValidationException,
        environment: DataFetchingEnvironment
    ): GraphQLError {
        return generateGraphQLTypedValidationError(
            typedValidationException,
            typedValidationException.message,
            ErrorType.BAD_REQUEST,
            environment
        )
    }

    @GraphQlExceptionHandler(DataIntegrityViolationException::class)
    fun handleDataIntegrityViolationException(
        dataIntegrityViolationEx: DataIntegrityViolationException,
        environment: DataFetchingEnvironment
    ): GraphQLError {
        val constraintViolationEx: ConstraintViolationException? =
            getConstraintViolationException(dataIntegrityViolationEx.cause)

        return if (constraintViolationEx != null) {
            generateGraphQLError(
                if (constraintViolationEx.cause != null) constraintViolationEx.cause!! else constraintViolationEx,
                getConstraintViolationErrorMessage(constraintViolationEx),
                ErrorType.BAD_REQUEST,
                environment
            )
        } else {
            generateGraphQLError(
                if (dataIntegrityViolationEx.cause != null) dataIntegrityViolationEx.cause!! else dataIntegrityViolationEx,
                DEFAULT_CONSTRAINT_VIOLATION_ERROR_MESSAGE,
                ErrorType.BAD_REQUEST,
                environment
            )
        }
    }

    fun unwrapException(throwable: Throwable): Throwable =
        if ((throwable is IllegalStateException || throwable is UndeclaredThrowableException) && throwable.cause != null)
            throwable.cause!!
        else
            throwable

    fun generateGraphQLTypedValidationError(
        typedValidationException: TypedValidationException,
        errorMsg: String?,
        errorType: ErrorType,
        environment: DataFetchingEnvironment,
        extensions: Map<String, Any?> = mapOf()
    ): GraphQLError {
        val exceptionParameters =
            DataFetcherExceptionHandlerParameters.newExceptionParameters()
                .dataFetchingEnvironment(environment)
                .exception(typedValidationException)
                .build()

        val internalExtensions =
            mapOf("originalMessage" to typedValidationException.message.trim()).let {
                if (typedValidationException.typedValidationErrors.isEmpty()) it else {
                    it.plus(Pair("typedValidationErrors", typedValidationException.typedValidationErrors))
                }
            }

        return generateGraphQLErrorInternal(
            typedValidationException,
            errorMsg,
            errorType,
            exceptionParameters,
            extensions,
            internalExtensions
        )
    }

    fun generateGraphQLError(
        throwable: Throwable,
        errorMsg: String?,
        errorType: ErrorType,
        environment: DataFetchingEnvironment,
        extensions: Map<String, Any?> = mapOf()
    ): GraphQLError {
        val unwrappedException = unwrapException(throwable)

        val exceptionParameters =
            DataFetcherExceptionHandlerParameters.newExceptionParameters()
                .dataFetchingEnvironment(environment)
                .exception(unwrappedException)
                .build()

        val internalExtensions =
            mapOf("originalMessage" to unwrappedException.message?.trim()).let {
                if (exceptionParameters.argumentValues.isEmpty()) it else {
                    it.plus(Pair("argumentValues", exceptionParameters.argumentValues))
                }
            }

        return generateGraphQLErrorInternal(
            unwrappedException, errorMsg, errorType, exceptionParameters, extensions, internalExtensions
        )
    }

    private fun generateGraphQLErrorInternal(
        throwable: Throwable,
        errorMsg: String?,
        errorType: ErrorType,
        exceptionParameters: DataFetcherExceptionHandlerParameters,
        extensions: Map<String, Any?> = mapOf(),
        internalExtensions: Map<String, Any?> = mapOf(),
    ): GraphQLError {
        val internalServerError =
            getMessage(LocaleContextHolder.getLocale(), DEFAULT_ERROR_CODE, DEFAULT_ERROR_MESSAGE)

        val localizedErrorMessage = getMessage(
            LocaleContextHolder.getLocale(),
            errorMsg ?: DEFAULT_ERROR_CODE,
            internalServerError,
            if (throwable is ApplicationException) throwable.args else emptyArray()
        )

        logException(localizedErrorMessage, throwable)
        return GraphqlErrorBuilder.newError()
            .message(localizedErrorMessage)
            // Is this really necessary?
            //.locations(listOf(exceptionParameters.sourceLocation))
            .path(exceptionParameters.path)
            .extensions(extensions.plus(internalExtensions))
            .errorType(errorType)
            .build()
    }

    private fun getConstraintViolationException(throwable: Throwable?): ConstraintViolationException? {
        return when (throwable) {
            null -> null
            is ConstraintViolationException -> throwable
            else -> getConstraintViolationException(throwable.cause)
        }
    }

    private fun getConstraintViolationErrorMessage(ex: ConstraintViolationException): String {
        return when (ex.sqlState) {
            "23502" -> "not_null_" + ex.constraintName + "_exception"
            "23505" -> "unique_" + ex.constraintName + "_exception"
            else -> DEFAULT_CONSTRAINT_VIOLATION_ERROR_MESSAGE
        }
    }

    @Suppress("SameParameterValue")
    private fun getMessage(
        locale: Locale,
        code: String,
        defaultMessage: String,
        args: Array<Any>? = null
    ): String {
        return messageSource.getMessage(code, args, defaultMessage, locale)!!
    }

    /**
     * Called to log the exception - a subclass could choose to something different in logging terms
     *
     * @param errorMsg  the graphql error message.
     * @param exception the exception that happened.
     */
    fun logException(errorMsg: String, exception: Throwable?) {
        logNotSafe.error(errorMsg, exception)
    }
}

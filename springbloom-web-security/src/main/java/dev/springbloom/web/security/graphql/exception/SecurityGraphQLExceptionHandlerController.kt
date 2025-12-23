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
package dev.springbloom.web.security.graphql.exception

import dev.springbloom.web.graphql.exception.GraphQLExceptionHandlerController
import graphql.GraphQLError
import graphql.schema.DataFetchingEnvironment
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication
import org.springframework.context.MessageSource
import org.springframework.graphql.data.method.annotation.GraphQlExceptionHandler
import org.springframework.graphql.execution.ErrorType
import org.springframework.security.authorization.AuthorizationDeniedException
import org.springframework.stereotype.Controller
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.ControllerAdvice

@Validated
@ConditionalOnWebApplication
@ControllerAdvice(annotations = [Controller::class])
class SecurityGraphQLExceptionHandlerController(
    messageSource: MessageSource
) : GraphQLExceptionHandlerController(messageSource) {

    @GraphQlExceptionHandler(AuthorizationDeniedException::class)
    fun handleException(
        authorizationDeniedException: AuthorizationDeniedException,
        environment: DataFetchingEnvironment
    ): GraphQLError {
        return super.generateGraphQLError(
            authorizationDeniedException,
            "unauthorized_access_exception",
            ErrorType.UNAUTHORIZED,
            environment,
            mapOf()
        )
    }

    override fun logException(errorMsg: String, exception: Throwable?) {
        if (exception !is AuthorizationDeniedException) {
            super.logException(errorMsg, exception)
        }
    }
}

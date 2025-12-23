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
@file:OptIn(ExperimentalTypeInference::class, ExperimentalContracts::class)
@file:Suppress("unused")

package dev.springbloom.core.validation

import arrow.core.Either
import arrow.core.flatten
import arrow.core.raise.Raise
import arrow.core.raise.RaiseDSL
import arrow.core.raise.fold
import com.natpryce.hamkrest.MatchResult
import com.natpryce.hamkrest.Matcher
import com.natpryce.hamkrest.describe
import dev.springbloom.core.containsAll
import dev.springbloom.core.not
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind.AT_MOST_ONCE
import kotlin.contracts.contract
import kotlin.experimental.ExperimentalTypeInference

interface TypedValidationEntity {
    fun isValid(): Either<TypedValidationException, Unit>
}

/**
 * Just an alias for the `Either<L, R>.getOrNull()` function.
 */
fun <L, R> Either<L, R>.rightOrNull(): R? {
    return this.getOrNull()
}

@RaiseDSL
inline fun ensureOnce(
    block: TypedValidationException.() -> (TypedValidationException.() -> List<TypedValidationError>?)
): Either<TypedValidationException, Unit> = ensureOnce("", block)


@RaiseDSL
inline fun ensureOnce(
    prefix: String,
    block: TypedValidationException.() -> (TypedValidationException.() -> List<TypedValidationError>?)
): Either<TypedValidationException, Unit> {
    contract { callsInPlace(block, AT_MOST_ONCE) }
    return fold({
        val ex = TypedValidationException(prefix)
        val typedErrors = block(ex).let {
            it(ex)?.toSet() ?: setOf()
        }

        if (typedErrors.isNotEmpty()) {
            ex.typedValidationErrors.addAll(typedErrors)
            raise(ex)
        }
        Unit
    }, { Either.Left(it) }, { Either.Right(it) })
}

@RaiseDSL
fun ensure(
    block: TypedValidationException.() -> (List<TypedValidationException.() -> List<TypedValidationError>?>)
): Either<TypedValidationException, Unit> = ensure("", block)


@RaiseDSL
fun ensure(
    prefix: String,
    block: TypedValidationException.() -> (List<TypedValidationException.() -> List<TypedValidationError>?>)
): Either<TypedValidationException, Unit> {
    contract { callsInPlace(block, AT_MOST_ONCE) }
    return fold({
        val ex = TypedValidationException(prefix)
        val typedErrors =
            block(ex).mapNotNull {
                it(ex)
            }.flatten().toSet()

        if (typedErrors.isNotEmpty()) {
            ex.typedValidationErrors.addAll(typedErrors)
            raise(ex)
        }
        Unit
    }, { Either.Left(it) }, { Either.Right(it) })
}

@RaiseDSL
fun Raise<TypedValidationException>.ensure(
    actions: List<TypedValidationException.() -> List<TypedValidationError>?>
) = ensure("", actions)

@RaiseDSL
fun Raise<TypedValidationException>.ensure(
    prefix: String,
    actions: List<TypedValidationException.() -> List<TypedValidationError>?>
) {
    val ex = TypedValidationException(prefix)
    val errors =
        actions.mapNotNull {
            it(ex)
        }.flatten().toSet()

    if (errors.isNotEmpty()) {
        ex.typedValidationErrors.addAll(errors)
        raise(ex)
    }
}

@Suppress("UnusedReceiverParameter")
fun Raise<TypedValidationException>.all(
    vararg elements: TypedValidationException.() -> List<TypedValidationError>?
): MutableList<TypedValidationException.() -> List<TypedValidationError>?> = mutableListOf(*elements)

@Suppress("UnusedReceiverParameter")
fun TypedValidationException.all(
    vararg elements: TypedValidationException.() -> List<TypedValidationError>?
): MutableList<TypedValidationException.() -> List<TypedValidationError>?> = mutableListOf(*elements)

@Suppress("UnusedReceiverParameter")
fun Raise<TypedValidationException>.allThese(
    vararg elements: List<TypedValidationException.() -> List<TypedValidationError>?>
): MutableList<TypedValidationException.() -> List<TypedValidationError>?> {
    val mutableList =
        mutableListOf<TypedValidationException.() -> List<TypedValidationError>?>()

    elements.forEach { mutableList.addAll(it) }
    return mutableList
}

@Suppress("UnusedReceiverParameter")
fun TypedValidationException.allThese(
    vararg elements: List<TypedValidationException.() -> List<TypedValidationError>?>
): MutableList<TypedValidationException.() -> List<TypedValidationError>?> {
    val mutableList =
        mutableListOf<TypedValidationException.() -> List<TypedValidationError>?>()

    elements.forEach { mutableList.addAll(it) }
    return mutableList
}

fun MutableList<TypedValidationException.() -> List<TypedValidationError>?>.and(
    validation: Either<TypedValidationException, Unit>
): MutableList<TypedValidationException.() -> List<TypedValidationError>?> {
    this.add {
        validation.fold(
            ifLeft = { left -> left.typedValidationErrors },
            ifRight = { null }
        )?.toList()
    }
    return this
}

fun MutableList<TypedValidationException.() -> List<TypedValidationError>?>.and(
    vararg elements: TypedValidationException.() -> List<TypedValidationError>?
): MutableList<TypedValidationException.() -> List<TypedValidationError>?> {
    this.addAll(elements)
    return this
}

fun MutableList<TypedValidationException.() -> List<TypedValidationError>?>.andThese(
    vararg elements: List<TypedValidationException.() -> List<TypedValidationError>?>
): MutableList<TypedValidationException.() -> List<TypedValidationError>?> {
    elements.forEach { this.addAll(it) }
    return this
}

@Suppress("UnusedReceiverParameter")
inline fun TypedValidationException.isValidOnce(
    condition: Boolean,
    crossinline errorMessage: () -> String
): TypedValidationException.() -> List<TypedValidationError>? {
    return { isValid(condition, errorMessage) }
}

@Suppress("UnusedReceiverParameter")
inline fun TypedValidationException.isValid(
    condition: Boolean,
    crossinline errorMessage: () -> String
): List<TypedValidationError>? {
    contract {
        callsInPlace(errorMessage, AT_MOST_ONCE)
        returns(null) implies condition
    }
    return if (condition) null else listOf(TypedValidationError(errorMessage()))
}

@Suppress("UnusedReceiverParameter")
inline fun <Entity : TypedValidationEntity?> TypedValidationException.isValidOnce(
    @BuilderInference entity: Entity,
    crossinline prefix: () -> String = { "" }
): TypedValidationException.() -> List<TypedValidationError>? {
    return { isValid(entity, prefix) }
}

@Suppress("UnusedReceiverParameter")
inline fun <Entity : TypedValidationEntity?> TypedValidationException.isValid(
    @BuilderInference entity: Entity,
    crossinline prefix: () -> String = { "" }
): List<TypedValidationError>? {
    return entity?.isValid()?.fold(
        ifLeft = { left ->
            left.typedValidationErrors.map { "${prefix()}:${it.errorMessage}" }
        },
        ifRight = { null }
    )?.map {
        TypedValidationError(it)
    }?.toMutableList()
}

@Suppress("UnusedReceiverParameter")
inline fun TypedValidationException.isValidOnce(
    validationEntities: Iterable<TypedValidationEntity>,
    crossinline prefix: () -> String = { "" }
): TypedValidationException.() -> List<TypedValidationError>? {
    return { isValid(validationEntities, prefix) }
}

@Suppress("UnusedReceiverParameter")
inline fun TypedValidationException.isValid(
    validationEntities: Iterable<TypedValidationEntity>,
    crossinline prefix: () -> String = { "" }
): List<TypedValidationError>? {
    return validationEntities.mapIndexed { index, entity ->
        entity.isValid().fold(
            ifLeft = { left ->
                left.typedValidationErrors.map { "${prefix()}[$index]${it.errorMessage}" }
            },
            ifRight = { null }
        )
    }.firstOrNull()?.map {
        TypedValidationError(it)
    }?.toList()
}

// Auxiliary test functions to use with Hamkrest!

abstract class HasValidationErrorsMatcher(
    private vararg val expectedErrors: String
) : Matcher<TypedValidationEntity> {

    override fun invoke(actual: TypedValidationEntity): MatchResult {
        return actual.isValid().fold(
            ifLeft = { ex ->
                if (invalid(ex.typedValidationErrors, *expectedErrors)) {
                    MatchResult.Mismatch("was ${describe(ex.typedValidationErrors.map { it.errorMessage })}")
                } else {
                    MatchResult.Match
                }
            },
            ifRight = { MatchResult.Match }
        )
    }

    abstract fun invalid(actualErrors: MutableList<TypedValidationError>, vararg expectedErrors: String): Boolean

    override val description: String get() = "contains ${describe(expectedErrors.toList())}"

    override val negatedDescription: String get() = "does not contain ${describe(expectedErrors.toList())}"
}

fun hasNoValidationErrors(): Matcher<TypedValidationEntity> = object : HasValidationErrorsMatcher() {

    override fun invalid(actualErrors: MutableList<TypedValidationError>, vararg expectedErrors: String): Boolean =
        actualErrors.isEmpty() && expectedErrors.isEmpty()
}

fun hasValidationErrors(
    vararg expectedErrors: String
): Matcher<TypedValidationEntity> = object : HasValidationErrorsMatcher(*expectedErrors) {

    override fun invalid(actualErrors: MutableList<TypedValidationError>, vararg expectedErrors: String): Boolean =
        not { actualErrors.map { it.errorMessage }.containsAll(*expectedErrors) }
}

fun hasExactValidationErrors(
    vararg expectedErrors: String
): Matcher<TypedValidationEntity> = object : HasValidationErrorsMatcher(*expectedErrors) {

    override fun invalid(actualErrors: MutableList<TypedValidationError>, vararg expectedErrors: String): Boolean {
        return actualErrors.size != expectedErrors.size
            || not { actualErrors.map { it.errorMessage }.containsAll(*expectedErrors) }
    }
}

abstract class HasErrorsMatcher(
    private vararg val expectedErrors: String
) : Matcher<Either<TypedValidationException, *>> {

    override fun invoke(actual: Either<TypedValidationException, *>): MatchResult {
        return actual.fold(
            ifLeft = { ex ->
                if (invalid(ex.typedValidationErrors, *expectedErrors)) {
                    MatchResult.Mismatch("was ${describe(ex.typedValidationErrors.map { it.errorMessage })}")
                } else {
                    MatchResult.Match
                }
            },
            ifRight = { MatchResult.Match }
        )
    }

    abstract fun invalid(actualErrors: MutableList<TypedValidationError>, vararg expectedErrors: String): Boolean

    override val description: String get() = "contains ${describe(expectedErrors.toList())}"

    override val negatedDescription: String get() = "does not contain ${describe(expectedErrors.toList())}"
}

fun hasNoErrors(): Matcher<Either<TypedValidationException, *>> = object : HasErrorsMatcher() {

    override fun invalid(actualErrors: MutableList<TypedValidationError>, vararg expectedErrors: String): Boolean =
        actualErrors.isEmpty() && expectedErrors.isEmpty()
}

fun hasErrors(
    vararg expectedErrors: String
): Matcher<Either<TypedValidationException, *>> = object : HasErrorsMatcher(*expectedErrors) {

    override fun invalid(actualErrors: MutableList<TypedValidationError>, vararg expectedErrors: String): Boolean =
        not { actualErrors.map { it.errorMessage }.containsAll(*expectedErrors) }
}

fun hasExactErrors(
    vararg expectedErrors: String
): Matcher<Either<TypedValidationException, *>> = object : HasErrorsMatcher(*expectedErrors) {

    override fun invalid(actualErrors: MutableList<TypedValidationError>, vararg expectedErrors: String): Boolean {
        return actualErrors.size != expectedErrors.size
            || not { actualErrors.map { it.errorMessage }.containsAll(*expectedErrors) }
    }
}

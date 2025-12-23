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
package dev.springbloom.core.validation

import arrow.core.Either
import arrow.core.raise.either
import com.natpryce.hamkrest.assertion.assertThat
import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.specification.describe

@Suppress("unused")
class TypedValidationEntityTest :
    Spek({
        val `test object with blank name` = TypedValidationTestObject("")
        val `test object with invalid name` = TypedValidationTestObject("Not a Test")
        val `test object` = TypedValidationTestObject("Test") // Only value possible!

        describe("O exemplo de teste") {
            it("não pode estar com nome em branco") {
                assertThat(`test object with blank name`,
                    hasExactValidationErrors("invalidName", "invalidNameLength")
                )
            }

            it("está com nome inválido") {
                assertThat(`test object with invalid name`,
                    hasExactValidationErrors("invalidName", "invalidNameLength")
                )
            }

            it("está válido") {
                assertThat(`test object`, hasNoValidationErrors())
            }
        }

        val `blank address` = Address(fullAddress = "")
        val `valid address` = Address("Work", "Rua do Ouvidor, 100")
        describe("O endereço") {
            it("não pode estar em branco") {
                assertThat(`blank address`,
                    hasExactValidationErrors("BlankFullAddress")
                )
            }

            it("está válido") {
                assertThat(`valid address`, hasNoValidationErrors())
            }
        }

        val `invalid author` = Author("", setOf(`blank address`))
        val `valid author` = Author("Felipe Desiderati", setOf(`valid address`))
        describe("Os dados de autor") {
            it("estão inválidos") {
                assertThat(`invalid author`,
                    hasExactValidationErrors(
                        "BlankAuthorName",
                        "Addresses[0]BlankFullAddress"
                    )
                )
            }

            it("estão válidos") {
                assertThat(`valid author`, hasNoValidationErrors())
            }
        }

        val `invalid author with validation on constructor` =
            AuthorWithValidationOnConstructor("", setOf(`blank address`))

        val `valid with validation on constructor` =
            AuthorWithValidationOnConstructor("Felipe Desiderati", setOf(`valid address`))

        describe("Os dados de autor") {
            it("estão inválidos") {
                assertThat(`invalid author with validation on constructor`,
                    hasExactErrors(
                        "BlankAuthorName",
                        "Addresses[0]BlankFullAddress"
                    )
                )
            }

            it("estão válidos") {
                assertThat(`valid with validation on constructor`, hasNoErrors())
            }
        }

        val `invalid book` = Book("", setOf(`invalid author`))
        describe("Os dados de livro") {
            it("estão inválidos") {
                assertThat(`invalid book`,
                    hasExactValidationErrors(
                        "Book.BlankBookTitle",
                        "Book.Authors[0]BlankAuthorName",
                        "Book.Authors[0]Addresses[0]BlankFullAddress"
                    )
                )
            }
        }

        val `invalid book  with validation on constructor` =
            BookWithValidationOnConstructor("", setOf(`invalid author`))

        describe("Os dados de livro") {
            it("estão inválidos") {
                assertThat(`invalid book  with validation on constructor`,
                    hasExactErrors(
                        "Book.BlankBookTitle",
                        "Book.Authors[0]BlankAuthorName",
                        "Book.Authors[0]Addresses[0]BlankFullAddress"
                    )
                )
            }
        }
    })

data class Address(
    private val name: String = "Default Address",
    private val fullAddress: String
) : TypedValidationEntity {
    override fun isValid(): Either<TypedValidationException, Unit> =
        ensureOnce {
            isValidOnce(fullAddress.isNotBlank()) { "BlankFullAddress" }
        }
}

data class Author(
    val name: String,
    val addresses: Set<Address>
) : TypedValidationEntity {
    override fun isValid(): Either<TypedValidationException, Unit> =
        ensure {
            all(
                { isValid(name.isNotBlank()) { "BlankAuthorName" } },
                { isValid(addresses) { "Addresses" } }
            )
        }
}

// This is more appropriate for classes with immutable properties.
@Suppress("unused")
class AuthorWithValidationOnConstructor private constructor(
    val name: String,
    val addresses: Set<Address>
) {
    companion object {
        operator fun invoke(
            name: String,
            addresses: Set<Address>
        ): Either<TypedValidationException, AuthorWithValidationOnConstructor> =
            either {
                ensure(all(
                    { isValid(name.isNotBlank()) { "BlankAuthorName" } },
                    { isValid(addresses) { "Addresses" } }
                ))
                AuthorWithValidationOnConstructor(name, addresses)
            }
    }
}

data class Book(
    val title: String,
    val authors: Set<Author>
) : TypedValidationEntity {
    override fun isValid(): Either<TypedValidationException, Unit> =
        ensure("Book") {
            all(
                { isValid(title.isNotBlank()) { "BlankBookTitle" } },
                { isValid(authors.isNotEmpty()) { "EmptyAuthorsList" } },
                { isValid(authors) { "Authors" } }
            )
        }
}

@Suppress("unused")
class BookWithValidationOnConstructor private constructor(
    val title: String,
    val authors: Set<Author>
) {
    companion object {
        operator fun invoke(
            title: String,
            authors: Set<Author>
        ): Either<TypedValidationException, BookWithValidationOnConstructor> =
            either {
                ensure("Book", all(
                    { isValid(title.isNotBlank()) { "BlankBookTitle" } },
                    { isValid(authors.isNotEmpty()) { "EmptyAuthorsList" } },
                    { isValid(authors) { "Authors" } }
                ))
                BookWithValidationOnConstructor(title, authors)
            }
    }
}

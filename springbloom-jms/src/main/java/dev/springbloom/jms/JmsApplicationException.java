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
package dev.springbloom.jms;

import dev.springbloom.core.exception.ApplicationException;
import jakarta.jms.Message;
import lombok.Getter;

import java.io.Serial;

@Getter
@SuppressWarnings("unused")
public class JmsApplicationException extends ApplicationException {

    @Serial
    private static final long serialVersionUID = 1L;

    private final Message failedMessage;

    public JmsApplicationException(Message failedMessage, String description) {
        super(description);
        this.failedMessage = failedMessage;
    }

    public JmsApplicationException(Message failedMessage, String description, Object... args) {
        super(description, args);
        this.failedMessage = failedMessage;
    }

    public JmsApplicationException(Message failedMessage, String description, Throwable cause) {
        super(description, cause);
        this.failedMessage = failedMessage;
    }

    public JmsApplicationException(
        Message failedMessage,
        String description,
        Throwable cause,
        Object... args
    ) {
        super(description, cause, args);
        this.failedMessage = failedMessage;
    }
}

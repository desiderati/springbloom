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
package dev.springbloom.web.notification.controller;

import dev.springbloom.web.notification.domain.BroadcastMessage;
import dev.springbloom.web.notification.service.NotificationService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

/**
 * When using the @Controller annotation, Spring does not automatically prefix routes with /api -
 * that behavior typically applies to @RestController combined with a global request mapping.
 */
@Controller
@ResponseBody
@RequestMapping("${spring.web.atmosphere.url.mapping:/atm}/broadcast")
public class BroadcastController {

    private final NotificationService notificationService;

    @Autowired
    public BroadcastController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @PostMapping
    public void broadcast(@RequestBody @Valid BroadcastMessage<?> broadcastMessage) {
        if (broadcastMessage.getResourceId() != null) {
            notificationService.broadcastToSpecificResource(
                broadcastMessage.getResourceId(),
                broadcastMessage.getPayload()
            );
        } else if (broadcastMessage.getBroadcastId() != null) {
            notificationService.broadcastToSpecificBroadcaster(
                broadcastMessage.getBroadcastId(),
                broadcastMessage.getPayload()
            );
        } else {
            notificationService.broadcastToAll(
                broadcastMessage.getPayload()
            );
        }
    }
}

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

package dev.springbloom.web.security.rest;

import org.springframework.boot.autoconfigure.security.oauth2.client.ConditionalOnOAuth2ClientRegistrationProperties;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.annotation.RegisteredOAuth2AuthorizedClient;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.client.RestClient;

import java.util.HashMap;
import java.util.Map;

@Controller
@ResponseBody
@RequestMapping("/oauth2")
@ConditionalOnOAuth2ClientRegistrationProperties
public class OAuth2UserController {

    private final RestClient restClient;

    public OAuth2UserController(RestClient restClient) {
        this.restClient = restClient;
    }

    @GetMapping("/me")
    public Map<String, Object> me(
        @RegisteredOAuth2AuthorizedClient OAuth2AuthorizedClient client,
        @AuthenticationPrincipal OAuth2User principal
    ) {
        Map<String, Object> response = new HashMap<>();
        response.put("accessToken", client.getAccessToken().getTokenValue());
        response.put("user", principal.getAttributes());
        return response;
    }

    @GetMapping("/userinfo")
    public Map<?, ?> userInfo() {
        return restClient.get()
            .uri("https://www.googleapis.com/oauth2/v3/userinfo")
            .retrieve()
            .body(Map.class);
    }
}

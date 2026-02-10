/*
 * # Copyright 2024-2025 NetCracker Technology Corporation
 * #
 * # Licensed under the Apache License, Version 2.0 (the "License");
 * # you may not use this file except in compliance with the License.
 * # You may obtain a copy of the License at
 * #
 * #      http://www.apache.org/licenses/LICENSE-2.0
 * #
 * # Unless required by applicable law or agreed to in writing, software
 * # distributed under the License is distributed on an "AS IS" BASIS,
 * # WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * # See the License for the specific language governing permissions and
 * # limitations under the License.
 *
 */

package org.qubership.automation.itf.ui.controls.service.token;

import java.util.Base64;

import org.apache.commons.lang3.NotImplementedException;
import org.qubership.automation.itf.ui.swagger.SwaggerConstants;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.tags.Tags;

@RestController
@Tags({
        @Tag(name = SwaggerConstants.TOKEN_GENERATOR_COMMAND_API,
                description = SwaggerConstants.TOKEN_GENERATOR_COMMAND_API_DESCR)
})
public class TokenGeneratorService {

    @RequestMapping(value = "/service/token", method = RequestMethod.POST)
    @Operation(summary = "GenerateToken", description = "Generate token of chosen type based on username/password "
            + "pair. Generated token can be used in further requests to authorize them by putting the token to the "
            + "\"Authorization\" header in request. Supported token types: Basic.",
            tags = {SwaggerConstants.TOKEN_GENERATOR_COMMAND_API})
    public String generateToken(@RequestBody TokenGeneratorRequest tokenGeneratorRequest) {
        return generateTokenFromRequest(tokenGeneratorRequest);
    }

    private String generateTokenFromRequest(TokenGeneratorRequest tokenGeneratorRequest) {
        TokenGeneratorRequest.TokenType tokenType =
                TokenGeneratorRequest.TokenType.fromText(tokenGeneratorRequest.getTokenType());
        switch (tokenType) {
            case BASIC:
                return tokenType.prefix() + " "
                        + Base64.getEncoder().encodeToString((tokenGeneratorRequest.getUsername() + ":"
                        + tokenGeneratorRequest.getPassword()).getBytes());
            case JWT:
                throw new NotImplementedException("JWT token generation is no supported");
            default:
                throw new IllegalArgumentException("Unknown token type exception");
        }
    }

}

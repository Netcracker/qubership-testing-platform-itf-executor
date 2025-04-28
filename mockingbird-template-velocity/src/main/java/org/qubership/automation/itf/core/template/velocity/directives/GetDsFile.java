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

package org.qubership.automation.itf.core.template.velocity.directives;

import java.io.IOException;
import java.io.Writer;
import java.util.UUID;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.velocity.context.InternalContextAdapter;
import org.apache.velocity.exception.MethodInvocationException;
import org.apache.velocity.exception.ParseErrorException;
import org.apache.velocity.exception.ResourceNotFoundException;
import org.apache.velocity.runtime.directive.Directive;
import org.apache.velocity.runtime.parser.node.Node;
import org.qubership.automation.itf.core.util.feign.http.HttpClientFactory;
import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;

public class GetDsFile extends Directive {

    private static String getExceptionMessage() {
        return "Directive '#get_ds_file': the 1st argument is $filePath, like "
                + "'/attachment/a1919c04-8f39-49a1-aae1-ee52c815a221', " + "the 2nd argument is $encoding (optional; "
                + "'UTF-8' by default)";
    }

    @Override
    public String getName() {
        return "get_ds_file";
    }

    @Override
    public int getType() {
        return LINE;
    }

    @Override
    public boolean render(InternalContextAdapter internalContextAdapter, Writer writer, Node node) throws IOException,
            ResourceNotFoundException, ParseErrorException, MethodInvocationException {
        String filePath;
        String encoding = "UTF-8";
        switch (node.jjtGetNumChildren()) {
            case 1:
                filePath = getString(node.jjtGetChild(0), internalContextAdapter);
                break;
            case 2:
                filePath = getString(node.jjtGetChild(0), internalContextAdapter);
                encoding = getString(node.jjtGetChild(1), internalContextAdapter);
                if (StringUtils.isBlank(encoding)) {
                    encoding = "UTF-8";
                }
                break;
            default:
                // It's discussable: to log warn or to throw an exception
                rsvc.getLog().warn("Incorrect directive usage format (#get_ds_file). " + getExceptionMessage());
                return true;
        }
        // It's supposed that filePath should be like "/attachment/a1919c04-8f39-49a1-aae1-ee52c815a221",
        // this is the format of DSS file variable value returned from DSS where DS contents are got
        UUID dataSetUuid
                = UUID.fromString(filePath.replace("/attachment/", ""));
        ResponseEntity<Resource> responseEntity = HttpClientFactory.getDatasetsAttachmentFeignClient()
                .getAttachmentByParameterId(dataSetUuid);

        if (!responseEntity.hasBody()) {
            throw new IOException(String.format("Response body is null for '%s', http status %s.",
                    filePath, responseEntity.getStatusCode()));
        }
        writer.append(IOUtils.toString(responseEntity.getBody().getInputStream(), encoding));
        return true;
    }

    private String getString(Node node, InternalContextAdapter internalContextAdapter) {
        if (node != null) {
            return String.valueOf(node.value(internalContextAdapter));
        } else {
            return null;
        }
    }
}

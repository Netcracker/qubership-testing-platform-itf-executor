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

package org.qubership.automation.itf.transport.http;

import static org.apache.http.entity.ContentType.MULTIPART_FORM_DATA;
import static org.qubership.automation.itf.transport.camel.Helper.GSON;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.activation.DataHandler;
import javax.activation.FileDataSource;
import javax.activation.URLDataSource;
import javax.servlet.ServletRequest;
import javax.servlet.http.HttpServletRequest;

import org.apache.camel.Exchange;
import org.apache.camel.StringSource;
import org.apache.camel.component.cxf.CxfPayload;
import org.apache.camel.component.http4.HttpComponent;
import org.apache.camel.impl.DefaultHeaderFilterStrategy;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.mime.HttpMultipartMode;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.entity.mime.content.ByteArrayBody;
import org.apache.http.entity.mime.content.FileBody;
import org.apache.http.impl.client.CloseableHttpClient;
import org.qubership.automation.itf.core.util.config.ApplicationConfig;
import org.qubership.automation.itf.core.util.feign.http.HttpClientFactory;
import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;

public class Helper {

    /*  There is a problem when one tests ITF running on the localhost from the localhost too.
     *   For example, sends REST/SOAP-requests from Postman to local ITF.
     *   In that case, both methods .getRemoteAddr() and .getRemoteHost() return 0:0:0:0:0:0:0:1
     *   Why? Please see an explanation here: https://stackoverflow
     * .com/questions/17964297/using-request-getremoteaddr-returns-00000001
     *
     *   The problem is:
     *       If we use this address to send extra request (after normal response) to the address of the  sender,
     *       endpointUrl is, for example, http://0:0:0:0:0:0:0:1:9010/NokiaFO/Notification,
     *       and Camel software (Camel version is 2.17.0) creates Endpoint incorrectly.
     *       Endpoint points to URL: http://NokiaFO/Notification - it's definitely incorrect.
     *       So, exchange fails with 'invalid Endpoint' exception
     *
     *   I suppose (after consulting Roman Barmin): replace "0:0:0:0:0:0:0:1" to "localhost".
     */

    /**
     * TODO Add JavaDoc.
     */
    public static void addClientCoordsToHeaders(Map<String, Object> headers, ServletRequest servletRequest) {
        if (Objects.nonNull(servletRequest)) {
            String ipAddress = getCorrectedAddress(servletRequest.getRemoteAddr());
            headers.put("client", ipAddress); // Do NOT delete for backward compatibility
            headers.put("remoteAddr", ipAddress);
            headers.put("remoteHost", getCorrectedAddress(servletRequest.getRemoteHost()));
            headers.put("remotePort", servletRequest.getRemotePort());
            headers.put("protocol", servletRequest.getProtocol());
        }
    }

    /**
     * Primarily, it's used in the REST/SOAP triggers. Copied from atp-itf. May be to be deleted soon.
     *
     * @param headers        collection with headers
     * @param servletRequest request received
     */
    public static void fixCoNamedHeaders(Map<String, Object> headers, ServletRequest servletRequest) {
        if (Objects.nonNull(servletRequest)) {
            Enumeration<String> names = ((HttpServletRequest) servletRequest).getHeaderNames();
            while (names.hasMoreElements()) {
                String curName = names.nextElement();
                Enumeration<String> requestHeaders = ((HttpServletRequest) servletRequest).getHeaders(curName);
                List<String> list = new ArrayList<>();
                while (requestHeaders.hasMoreElements()) {
                    list.add(requestHeaders.nextElement());
                }
                if (list.size() > 1) {
                    headers.replace(curName, list);
                }
            }
        }
    }

    /**
     * TODO Add JavaDoc.
     */
    public static org.apache.camel.Message composeBodyForSOAPOutbound(
            org.apache.camel.Message camelMessage,
            org.qubership.automation.itf.core.model.jpa.message.Message itfMessage) {
        turnOffTransferEncodingChunkedHeader(camelMessage);
        camelMessage.setBody(itfMessage.getText());
        return camelMessage;
    }

    /**
     * TODO Add JavaDoc.
     */
    public static org.apache.camel.Message composeBodyForSOAPInbound(
            org.apache.camel.Message camelMessage,
            org.qubership.automation.itf.core.model.jpa.message.Message itfMessage,
            boolean isRawDataformat) {
        turnOffTransferEncodingChunkedHeader(camelMessage);
        if (!StringUtils.isBlank(itfMessage.getText())) {
            if (isRawDataformat) {
                camelMessage.setBody(itfMessage.getText());
            } else {
                StringSource stringSource = new StringSource(itfMessage.getText());
                List<StringSource> list = new ArrayList<>();
                list.add(stringSource);
                CxfPayload cxp = new CxfPayload(null, list, null);
                camelMessage.setBody(cxp);
            }
        }
        return camelMessage;
    }

    /**
     * TODO Add JavaDoc.
     */
    public static org.apache.camel.Message composeBodyForREST(
            org.apache.camel.Message camelMessage,
            org.qubership.automation.itf.core.model.jpa.message.Message itfMessage) {
        String contentTypeString = camelMessage.getHeader(Exchange.CONTENT_TYPE).toString();
        ContentType contentType = ContentType.parse(contentTypeString);
        if (contentType.getCharset() != null) {
            camelMessage.getExchange().setProperty("CamelCharsetName", contentType.getCharset().toString());
        }
        Object contentDisposition = camelMessage.getHeader("Content-Disposition");
        if (contentType.getMimeType().equals(MULTIPART_FORM_DATA.getMimeType())) {
            return composeMultipartBody(camelMessage, itfMessage, contentType, (String) contentDisposition);
        } else if (contentDisposition != null && (contentDisposition.toString().startsWith("attachment")
                || contentDisposition.toString().startsWith("inline"))) {
            return composeAttachmentsBody(camelMessage, itfMessage);
        } else if (contentTypeString.startsWith("application/graphql")) {
            return graphqlToJson(camelMessage, itfMessage, contentTypeString);
        } else {
            /* Default composition */
            turnOffTransferEncodingChunkedHeader(camelMessage);
            camelMessage.setBody(itfMessage.getText());
            return camelMessage;
        }
    }

    public static void clearForbiddenHeaders(HttpComponent httpComponent,
                                             DefaultHeaderFilterStrategy itfHeaderFilterStrategy) {
        httpComponent.setHeaderFilterStrategy(itfHeaderFilterStrategy);
    }

    /*  Compose multipart/form-data message,
     *   containing (currently) only one part - parsed template content
     * */
    private static org.apache.camel.Message composeMultipartBody(
            org.apache.camel.Message camelMessage,
            org.qubership.automation.itf.core.model.jpa.message.Message itfMessage,
            ContentType contentType,
            String contentDisposition) {
        if (camelMessage.getHeader("filename") == null && camelMessage.getHeader("partname") == null) {
            camelMessage.setBody(itfMessage.getText());
            return camelMessage;
        }
        MultipartEntityBuilder multipartEntityBuilder = MultipartEntityBuilder.create();
        multipartEntityBuilder.setMode(HttpMultipartMode.BROWSER_COMPATIBLE);
        String boundary = contentType.getParameter("boundary");
        if (!StringUtils.isBlank(boundary)) {
            multipartEntityBuilder.setBoundary(boundary);
        }
        String filename;
        String partname;
        CloseableHttpResponse response = null;
        try {
            filename = (String) (camelMessage.getHeader("filename"));
            partname = (String) (camelMessage.getHeader("partname"));
            if (StringUtils.isBlank(partname)) {
                partname = "file";
            }
            /* Source variants:
                1. Source file is located anywhere in the web - if <filename> value is URL
                2. Source file is located in the filesystem - if <filename> value is NOT valid URL but not empty
                3. No source file; template is the source - if <filename> value is empty or null
            */
            if (StringUtils.isBlank(filename)) {
                String partFilename = getPartFilename(contentDisposition);
                File tmpfile = File.createTempFile(partFilename, ".tmp");
                tmpfile.deleteOnExit();
                BufferedWriter writer = new BufferedWriter(new FileWriter(tmpfile));
                writer.write(itfMessage.getText());
                writer.close();
                multipartEntityBuilder.addPart(partname, new FileBody(tmpfile, contentType, partFilename));
            } else {
                try {
                    URL url = new URL(filename);
                    multipartEntityBuilder.addPart(partname, new ByteArrayBody(
                            camelMessage.getExchange().getContext().getTypeConverter().convertTo(
                                    byte[].class, getViaClient(url)
                            ), getPartFilename(contentDisposition)));
                } catch (MalformedURLException ex) {
                    multipartEntityBuilder.addPart(partname, new FileBody(new File(filename)));
                }
            }
            camelMessage.setBody(multipartEntityBuilder.build());
            return camelMessage;
        } catch (Exception ex) {
            throw new RuntimeException("File exception while composing multipart message", ex);
        }
    }

    private static void turnOffTransferEncodingChunkedHeader(org.apache.camel.Message camelMessage) {
        /*
            To avoid EOFException or infinite looping while copying the empty input stream into output stream.
            See:
            /org/apache/tomcat/util/net/NioEndpoint.java#fillReadBuffer
            org/apache/camel/http/common/DefaultHttpBinding.java#doWriteDirectResponse and #checkChunked
            (camel-http4-common-2.20.4)
            I think this header is 'technical' (or ~inner) and should not be copied into monitoring data
         */
        /*
            Also:
                To avoid duplicated headers Transfer-Encoding: chunked, sent in the ITF stub response
         */
        camelMessage.setHeader(Exchange.HTTP_CHUNKED, false);
    }

    private static String getPartFilename(String contentDisposition) {
        if (StringUtils.isBlank(contentDisposition)) {
            return "file";
        }
        int i = contentDisposition.indexOf("filename=\"");
        if (i < 0) {
            return "file";
        }
        return contentDisposition.substring(i + 10, contentDisposition.indexOf("\"", i + 10));
    }

    /*  Compose message with binary body,
     *   containing (currently) only one part - from file attachment
     * */
    private static org.apache.camel.Message composeAttachmentsBody(
            org.apache.camel.Message camelMessage,
            org.qubership.automation.itf.core.model.jpa.message.Message itfMessage) {
        String filename = (String) (camelMessage.getHeader("filename"));
        InputStream inputStream = null;
        try {
            URL url = new URL(filename);
            if (url.getProtocol().equals("https")) {
                inputStream = getViaClient(url);
            }
            camelMessage.addAttachment("fileAttachment", new DataHandler(new URLDataSource(url)));
        } catch (MalformedURLException ex) {
            camelMessage.addAttachment("fileAttachment", new DataHandler(new FileDataSource(filename)));
        } catch (Exception ex) {
            throw new RuntimeException("HTTPClient exception while composing attachments message", ex);
        }
        try {
            byte[] data = camelMessage.getExchange().getContext().getTypeConverter().convertTo(byte[].class,
                    (inputStream == null)
                            ? camelMessage.getAttachment("fileAttachment").getInputStream()
                            : inputStream);
            camelMessage.setBody(data);
            return camelMessage;
        } catch (Exception ex) {
            throw new RuntimeException("File exception while composing attachments message", ex);
        }
    }

    /*
     *   The 1st implementation of graphQL --> json conversion.
     *   It works fine, but:
     *       1. It performs 5 subsequent replaces, each scans the whole string again and again...
     *           It could lead to performance problems on long strings
     *           (VIVO project sends graphQLs of 10K...100K...1M size)
     *       2. May be, there are some missed points necessary when a string is enclosed as json property value
     *
     *   So, I decided to create class GraphglQuery, initialize its instance and use Gson to convert it into json.
     *   It is performed in the graphql2json method below.
     * */
    private static String graphql2jsonOld(String graphqlString) {
        return "{\"query\" : \""
                + graphqlString
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\r", "\\r")
                .replace("\n", "\\n")
                .replace("\t", "\\t")
                + "\"}";
    }

    private static String graphql2json(String graphqlString) {
        return GSON.toJson(new GraphglQuery(graphqlString));
    }

    private static org.apache.camel.Message graphqlToJson(
            org.apache.camel.Message camelMessage,
            org.qubership.automation.itf.core.model.jpa.message.Message itfMessage,
            String sourceContentTypeString) {
        String targetContentTypeString = sourceContentTypeString
                .replace("application/graphql", "application/json");
        camelMessage.setHeader(Exchange.CONTENT_TYPE, targetContentTypeString);
        itfMessage.getHeaders().put(Exchange.CONTENT_TYPE, targetContentTypeString);
        camelMessage.setBody(graphql2json(itfMessage.getText()));
        return camelMessage;
    }

    private static InputStream getViaClient(URL url) throws IOException, URISyntaxException {
        String datasetServiceUrl = ApplicationConfig.env.getProperty("dataset.service.url");
        String publicGatewayUrl = ApplicationConfig.env.getProperty("atp.public.gateway.url");
        if ((StringUtils.isNotEmpty(datasetServiceUrl) && url.toString().startsWith(datasetServiceUrl))
                || (StringUtils.isNotEmpty(publicGatewayUrl) && url.toString().startsWith(publicGatewayUrl))) {
            UUID dataSetUuid = parsingStringToUuid(url);
            ResponseEntity<Resource> responseEntity = HttpClientFactory.getDatasetsAttachmentFeignClient()
                    .getAttachmentByParameterId(dataSetUuid);
            if (!responseEntity.hasBody()) {
                throw new IOException(String.format("Response body is null for '%s', http status %s.",
                        url, responseEntity.getStatusCode()));
            }
            return Objects.requireNonNull(responseEntity.getBody()).getInputStream();
        }
        CloseableHttpClient client = PreconfiguredHttpClientHolder.get();
        HttpGet request = new HttpGet(url.toURI());
        CloseableHttpResponse response = client.execute(request);
        int statusCode = response.getStatusLine().getStatusCode();
        if (statusCode >= HttpStatus.SC_MULTIPLE_CHOICES) {
            String body = StringUtils.EMPTY;
            if (response.getEntity() != null) {
                body = IOUtils.toString(response.getEntity().getContent(), StandardCharsets.UTF_8);
            }
            throw new IOException(String.format("Response is not accepted for '%s': %s [%s],\n body: %s",
                    url, response.getStatusLine().getReasonPhrase(), statusCode, body));
        }
        if (response.getEntity() == null) {
            throw new IOException(String.format("Response body is null for '%s': %s [%s]",
                    url, response.getStatusLine().getReasonPhrase(), statusCode));
        }
        return response.getEntity().getContent();
    }

    private static UUID parsingStringToUuid(URL url) {
        String path = url.getPath();

        if (StringUtils.isBlank(path)) {
            throw new NumberFormatException(String.format("An error occurred while parsing. "
                    + "File path [%s] cannot be empty.", url));
        }

        String uuidPattern = "[0-9a-f]{8}-[0-9a-f]{4}-[1-5][0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}";
        Pattern pattern = Pattern.compile(uuidPattern);
        Matcher matcher = pattern.matcher(path);

        if (!matcher.find()) {
            throw new NumberFormatException(String.format("An error occurred while parsing. "
                    + "Line [%s] missing UUID.", url));
        }
        String parsedUuid = matcher.group(0);
        return UUID.fromString(parsedUuid);
    }

    private static String getCorrectedAddress(String addr) {
        if (addr == null) {
            return null;
        }
        return (addr.equals("0:0:0:0:0:0:0:1")) ? "localhost" : addr;
    }
}

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

package org.qubership.automation.itf.transport.cli.outbound;

import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.assertTrue;

import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.annotation.Nonnull;

import org.apache.camel.CamelContext;
import org.apache.camel.Endpoint;
import org.apache.camel.Exchange;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.impl.DefaultCamelContext;
import org.apache.logging.log4j.util.Strings;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.qubership.automation.itf.core.model.jpa.message.Message;
import org.qubership.automation.itf.core.model.transport.ConnectionProperties;
import org.qubership.automation.itf.core.util.constants.PropertyConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CLIOutboundTransportTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(CLIOutboundTransportTest.class);

    private final String stringPemPrivateKey = "-----BEGIN RSA PRIVATE KEY-----\n"
            + "MIIEowIBAAKCAQEAt8iX3aT7fmi3DnXBa60uGFwOoBupETvw2UjjmIP+m9O8J4yR\n"
            + "5sLOg58nrtWiRY9Hm4zid29jzlvJobz+NMc9/geirw9Hu9sEmRz5J2qSXyO3xHbe\n"
            + "6uF2PQ/2BRHxx3XXcBRT4WGeBK/RglyScWQvj/IOx95gQ0qUcYtxYi82WdCqOyoR\n"
            + "KrGXAL/kmmBeIOD+E2gD6Ux8khpqHIo2ImRGxwLsog8fd4N0GGcoSkoKbQjmmPhj\n"
            + "KfNNJiZBj5mEXc+S/rHfF4ses2CY8w1w+YXIE5fi5ckUAMPQQyGVh0WiLTgi4oVG\n"
            + "w71MqvTiE1OuWxKmRL8oZ9NLDq+OT7wggPrcRQIDAQABAoIBAF17s4QSv7p1GWhJ\n"
            + "jbFvzdqmOOpIJ5+UldZwtRSHT6OD+FlFr5Fp1hItisnr8TbgwtPkve1yw5ncJpwW\n"
            + "vleqYWYuDBpv81Ui+xvHGRVrqDisunU84fcn6DT3QXUiw5Fp58QjEue59976b9+X\n"
            + "pwX2qBrYTZxtCRoxfYCkJXCEA2l7VSj65BwwQLQJA1ijVHaN/qsC1boRlDO/Dp/j\n"
            + "nTnTcdRpJ9TTqGnyJlnY9YCJEONeHrVqi0FHSmvh+W4n5muYvB4A2Ju+i9YfvHPC\n"
            + "zaq5JjrdnJfNkEoask0aMEhK6Mp/97cnN3draQSNeBwB7Sld6W6JBXPMQvW+1Cv+\n"
            + "qcgv7wECgYEA6/UQNm4ZWV+4wIFDHrIoddo6q2yn632wwsyrfj9Ksbsz6naFj4vZ\n"
            + "dlzTSyJz9E9KC+nCem4pYspwfTspWpVCLP17C9BOz2+48kUR6UeJE5Usf9Zlw2+i\n"
            + "HaMp21OOgVNfqbSNQsAi2YsixU7Aru0YIJmyMhGKq+yAXXaPZXKQ/JkCgYEAx2UA\n"
            + "oIDYpgklrdKoixsmdfgq0jascZWGM+Iw8MuwIawR4PL9guorfnhPvMSRc9UDlYr0\n"
            + "SpH+SoqNHhXjErfo8F3CO2/ZxV46lYMrAieFg3/NkHsBcqp7PHybp/+ITnSn++uQ\n"
            + "0Lnrqdz9sgs1fA5crIW9nnKnTXiMX4RvGP/XHI0CgYBf0fcxg2h76OloE5YkQwk3\n"
            + "QtYMg2V1tmcv1FnYPO+iWXltv4/hiVNYQ73yhx07m29ggx9dBJt96OPhl0Ll7DMh\n"
            + "fhaX55H5n08l43Kwn0JFV5DooTJWOWFGU9pNnRMD5c21ZwLuloQQf/Yw1hhdcR8Z\n"
            + "LhE1T/ZWdwZx7hGxiuLiEQKBgBSGRww+dw6QPnqoBoVbJBhclTvSOOnwNEI+9D61\n"
            + "GMo+hhCbspC5PgTkqYCK01YTBS1tgjvyzzQpEuGX6ynQGIA1hnrLxqTUUD93owOz\n"
            + "wcCJdUV8A+gjuE+/m94tJYC97VS3KM7zdFil0M906+p7J/ryQVSABMyqrfhfD3iJ\n"
            + "TUE5AoGBAKv16AMLDB+d0BWwrAwyl2/oNP7m2/ZukQxi4R7HbJtRDGOBA0daiXIL\n"
            + "ebo0X8fNU/Jo8+Gm5h1SbdnbKznWNX2dryGRbD8rlBbY4mjQyoVhvm8JumY4G7Kc\n"
            + "8kBLPb+k978OcUzRrd3qrg9pqMOLKlePOyNlMLMAriYjmMygoUH6\n"
            + "-----END RSA PRIVATE KEY-----";
    private ExecutorService executorService;
    private int read;
    private String requestMessage;

    @Nonnull
    private static Message getMessageFromBytes(Exchange exchange) {
        Message response;
        Object answerObject = exchange.getOut().getBody();
        ByteArrayInputStream answer = (ByteArrayInputStream) answerObject;
        int n = answer.available();
        if (n > 0) {
            byte[] bytes = new byte[n];
            int cnt = answer.read(bytes, 0, n);
            response = new Message(new String(bytes, 0, cnt, StandardCharsets.UTF_8));
        } else {
            response = new Message();
        }
        return response;
    }

    @Nonnull
    private static File getTempPemFile(String sshKey) throws IOException {
        File tmpfile = File.createTempFile(String.valueOf(System.currentTimeMillis()), ".pem");
        tmpfile.deleteOnExit();
        BufferedWriter writer = new BufferedWriter(new FileWriter(tmpfile));
        writer.write(sshKey);
        writer.close();
        return tmpfile;
    }

    @Before
    public void setUp() {
        executorService = Executors.newSingleThreadExecutor();
    }

    @After
    public void tearDown() {
        executorService.shutdownNow();
    }

    @Test()
    public void testConnectionTCP() throws Exception {
        executorService.execute(listener());
        CLIOutboundTransport transport = new CLIOutboundTransport();
        Message message = new Message();
        message.getConnectionProperties().put(PropertyConstants.Cli.REMOTE_IP, "127.0.0.1");
        message.getConnectionProperties().put(PropertyConstants.Cli.REMOTE_PORT, 15596);
        message.getConnectionProperties().put(PropertyConstants.Cli.CONNECTION_TYPE, "tcp");
        message.setText("Test");
        transport.send(message, "", UUID.randomUUID());
        assertTrue(read > 0);
        assertEquals(message.getText(), this.requestMessage);
    }

    private Thread listener() {
        return new Thread(() -> {
            try {
                ServerSocketChannel bind = ServerSocketChannel.open().bind(new InetSocketAddress(15596));
                while (!Thread.interrupted()) {
                    try {
                        SocketChannel channel = bind.accept();
                        channel.configureBlocking(true);
                        ByteBuffer allocate = ByteBuffer.allocate(1024);
                        read = channel.read(allocate);
                        requestMessage = new String(allocate.array()).trim();
                        channel.close();
                    } catch (Exception e) {
                        LOGGER.error("Failed reading data from socket", e);
                    }
                }
            } catch (Exception e) {
                LOGGER.warn("Something went wrong, please check", e);
            }
        });
    }

    public void manualTestSendMessageByCliSshTransportWithPemKey() throws Exception {
        CamelContext camelContext = new DefaultCamelContext();
        ProducerTemplate template = camelContext.createProducerTemplate();
        camelContext.start();
        ConnectionProperties properties = new ConnectionProperties();
        properties.put(PropertyConstants.Cli.CONNECTION_TYPE, "SSH");
        properties.put(PropertyConstants.Cli.SSH_KEY, stringPemPrivateKey);
        properties.put(PropertyConstants.Cli.REMOTE_IP, "dev-service-address");
        properties.put(PropertyConstants.Cli.REMOTE_PORT, "22");
        properties.put(PropertyConstants.Cli.USER, "some-user");
        properties.put(PropertyConstants.Cli.PASSWORD, null);

        String uri = resolveEndpoint(properties);
        Endpoint endPoint = camelContext.getEndpoint(uri);
        Exchange exchange = endPoint.createExchange();
        exchange.getIn().setBody("pwd");
        exchange = template.send(endPoint, exchange);
        Message response = getMessageFromBytes(exchange);
        Assert.assertEquals("/home/some-dir\n", response.getText());
    }

    private String resolveEndpoint(ConnectionProperties properties) throws IllegalArgumentException, IOException {
        boolean isSsh = ("SSH").equals(properties.get(PropertyConstants.Cli.CONNECTION_TYPE));
        Object user = properties.get(PropertyConstants.Cli.USER);
        Object password = properties.get(PropertyConstants.Cli.PASSWORD);
        Object sshKey = properties.get(PropertyConstants.Cli.SSH_KEY);
        boolean userIsBlank = Objects.isNull(user) || Strings.isBlank(String.valueOf(user));
        boolean passIsBlank = Objects.isNull(password) || Strings.isBlank(String.valueOf(password));
        boolean sshKeyIsBlank = Objects.isNull(sshKey) || Strings.isBlank(String.valueOf(sshKey));
        if (isSsh && passIsBlank && sshKeyIsBlank) {
            throw new IllegalArgumentException("'Password' and 'ssh_key' can't be empty! Please fill one of them.");
        }
        if (isSsh && userIsBlank) {
            throw new IllegalArgumentException("'User' can't be empty!");
        }
        return isSsh ? "ssh:" + properties.get(PropertyConstants.Cli.USER)
                        + ((passIsBlank) ? "@" : ':' + password.toString() + "@")
                        + properties.get(PropertyConstants.Cli.REMOTE_IP) + ":"
                        + properties.get(PropertyConstants.Cli.REMOTE_PORT)
                        + ((sshKeyIsBlank) ? "" : "?certResource=file:"
                            + getTempPemFile((String) properties.get(PropertyConstants.Cli.SSH_KEY)).getPath())
                : "netty4:" + properties.get(PropertyConstants.Cli.CONNECTION_TYPE) + "://"
                        + properties.get(PropertyConstants.Cli.REMOTE_IP) + ':'
                        + properties.get(PropertyConstants.Cli.REMOTE_PORT)
                        + "?textline=true&requestTimeout=5000";
    }

}

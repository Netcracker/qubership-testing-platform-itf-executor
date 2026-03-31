/*
 * # Copyright 2024-2026 NetCracker Technology Corporation
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

package org.qubership.automation.itf.transport.file.outbound;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.junit.Test;
import org.qubership.automation.itf.core.model.jpa.message.Message;
import org.qubership.automation.itf.core.util.constants.PropertyConstants;

import com.google.common.collect.Maps;

public class FileOutboundTest {

    private final String stringPemPrivateKey = """
            -----BEGIN RSA PRIVATE KEY-----
            MIIEowIBAAKCAQEAt8iX3aT7fmi3DnXBa60uGFwOoBupETvw2UjjmIP+m9O8J4yR
            5sLOg58nrtWiRY9Hm4zid29jzlvJobz+NMc9/geirw9Hu9sEmRz5J2qSXyO3xHbe
            6uF2PQ/2BRHxx3XXcBRT4WGeBK/RglyScWQvj/IOx95gQ0qUcYtxYi82WdCqOyoR
            KrGXAL/kmmBeIOD+E2gD6Ux8khpqHIo2ImRGxwLsog8fd4N0GGcoSkoKbQjmmPhj
            KfNNJiZBj5mEXc+S/rHfF4ses2CY8w1w+YXIE5fi5ckUAMPQQyGVh0WiLTgi4oVG
            w71MqvTiE1OuWxKmRL8oZ9NLDq+OT7wggPrcRQIDAQABAoIBAF17s4QSv7p1GWhJ
            jbFvzdqmOOpIJ5+UldZwtRSHT6OD+FlFr5Fp1hItisnr8TbgwtPkve1yw5ncJpwW
            vleqYWYuDBpv81Ui+xvHGRVrqDisunU84fcn6DT3QXUiw5Fp58QjEue59976b9+X
            pwX2qBrYTZxtCRoxfYCkJXCEA2l7VSj65BwwQLQJA1ijVHaN/qsC1boRlDO/Dp/j
            nTnTcdRpJ9TTqGnyJlnY9YCJEONeHrVqi0FHSmvh+W4n5muYvB4A2Ju+i9YfvHPC
            zaq5JjrdnJfNkEoask0aMEhK6Mp/97cnN3draQSNeBwB7Sld6W6JBXPMQvW+1Cv+
            qcgv7wECgYEA6/UQNm4ZWV+4wIFDHrIoddo6q2yn632wwsyrfj9Ksbsz6naFj4vZ
            dlzTSyJz9E9KC+nCem4pYspwfTspWpVCLP17C9BOz2+48kUR6UeJE5Usf9Zlw2+i
            HaMp21OOgVNfqbSNQsAi2YsixU7Aru0YIJmyMhGKq+yAXXaPZXKQ/JkCgYEAx2UA
            oIDYpgklrdKoixsmdfgq0jascZWGM+Iw8MuwIawR4PL9guorfnhPvMSRc9UDlYr0
            SpH+SoqNHhXjErfo8F3CO2/ZxV46lYMrAieFg3/NkHsBcqp7PHybp/+ITnSn++uQ
            0Lnrqdz9sgs1fA5crIW9nnKnTXiMX4RvGP/XHI0CgYBf0fcxg2h76OloE5YkQwk3
            QtYMg2V1tmcv1FnYPO+iWXltv4/hiVNYQ73yhx07m29ggx9dBJt96OPhl0Ll7DMh
            fhaX55H5n08l43Kwn0JFV5DooTJWOWFGU9pNnRMD5c21ZwLuloQQf/Yw1hhdcR8Z
            LhE1T/ZWdwZx7hGxiuLiEQKBgBSGRww+dw6QPnqoBoVbJBhclTvSOOnwNEI+9D61
            GMo+hhCbspC5PgTkqYCK01YTBS1tgjvyzzQpEuGX6ynQGIA1hnrLxqTUUD93owOz
            wcCJdUV8A+gjuE+/m94tJYC97VS3KM7zdFil0M906+p7J/ryQVSABMyqrfhfD3iJ
            TUE5AoGBAKv16AMLDB+d0BWwrAwyl2/oNP7m2/ZukQxi4R7HbJtRDGOBA0daiXIL
            ebo0X8fNU/Jo8+Gm5h1SbdnbKznWNX2dryGRbD8rlBbY4mjQyoVhvm8JumY4G7Kc
            8kBLPb+k978OcUzRrd3qrg9pqMOLKlePOyNlMLMAriYjmMygoUH6
            -----END RSA PRIVATE KEY-----""";

    @Test
    public void send() throws Exception {
        FileOutbound fileOutbound = new FileOutbound();
        Message message = new Message("Hello world");
        Map<String, Object> properties = Maps.newHashMap();
        properties.put(PropertyConstants.File.DESTINATION_FILE_NAME, "test.txt");
        properties.put(PropertyConstants.File.TYPE, "file");
        properties.put(PropertyConstants.File.PATH, "tmp");
        message.fillConnectionProperties(properties);
        fileOutbound.send(message, "TEST", UUID.randomUUID());
    }

    public void sendSftp() throws Exception {
        FileOutbound fileOutbound = new FileOutbound();
        Message message = new Message("Hello world2");
        Map<String, Object> properties = Maps.newHashMap();
        properties.put(PropertyConstants.File.DESTINATION_FILE_NAME, "some-file.txt");
        properties.put(PropertyConstants.File.TYPE, "sftp");
        properties.put(PropertyConstants.File.PATH, "tmp");
        properties.put(PropertyConstants.File.HOST, "dev-service-address");
        properties.put(PropertyConstants.File.PRINCIPAL, "some-user");
        properties.put(PropertyConstants.File.CREDENTIALS, "");
        List<String> key = new ArrayList<>();
        key.add(stringPemPrivateKey);
        properties.put(PropertyConstants.File.SSH_KEY, key);
        message.fillConnectionProperties(properties);
        fileOutbound.send(message, "TEST", UUID.randomUUID());
    }
}

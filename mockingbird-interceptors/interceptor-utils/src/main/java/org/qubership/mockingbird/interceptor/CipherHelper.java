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

package org.qubership.mockingbird.interceptor;

import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.util.ArrayList;
import java.util.List;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang3.StringUtils;
import org.qubership.automation.itf.core.model.interceptor.Interceptor;
import org.qubership.automation.itf.core.model.jpa.interceptor.InterceptorParams;
import org.qubership.automation.itf.core.util.constants.InterceptorConstants;
import org.qubership.automation.itf.core.util.descriptor.InterceptorPropertyDescriptor;

public class CipherHelper {

    public static final String PASSWORD = "Password";
    public static final String PASSWORD_DESCRIPTION = "Password for encoding/decoding.";
    public static final String CIPHER_ALGORITHM = "Cipher algorithm";
    public static final String CIPHER_ALGORITHM_DESCRIPTION = "Algorithm for encoding/decoding.";
    public static final String SYMMETRIC_BLOCK_SIZE = "Symmetric Block Size";
    public static final String SYMMETRIC_BLOCK_SIZE_DESCRIPTION = "Size of block for AES algorithm.";
    public static final String CIPHER_MODE = "Cipher mode";
    public static final String CIPHER_MODE_DESCRIPTION = "Mode for algorithm";
    public static final String CIPHER_PADDING = "Cipher padding";
    public static final String CIPHER_PADDING_DESCRIPTION = "Padding";
    public static final String INITIALIZATION_VECTOR = "Initialization Vector";
    public static final String INITIALIZATION_VECTOR_DESCRIPTION =
            "Initialization vector for symmetric algorithm. " + "This parameter isn't applicable for the \"ECB\" mode.";
    public static final String ENCODING = "Encoding";
    public static final String ENCODING_DESCRIPTION = "Encoding";

    private static final String AES = "AES";
    private static final String DES = "DES";
    private static final String BASE_64 = "Base64";
    private static final int DEFAULT_AES_BLOCK_SIZE = 256;
    private static final int DES_BLOCK_SIZE = 64;
    private static final int DESEDE_BLOCK_SIZE = 192;
    private static final byte[] DEFAULT_INITIALIZATION_VECTOR = new byte[]{0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
            0, 0};
    private static final int KEY_SPEC_ITERATION = 1024;
    private static final String SECRET_KEY_GENERATION_ALGORITHM = "PBKDF2WithHmacSHA1";

    private static final String[] CIPHER_ALGORITHM_OPTIONS = new String[]{"AES", "DES", "DESede"};
    private static final String[] CIPHER_MODE_OPTIONS = new String[]{"ECB", "CBC"};
    private static final String[] CIPHER_PADDING_OPTIONS = new String[]{"NoPadding", "PKCS5Padding"};
    private static final String[] SYMMETRIC_BLOCKSIZE_OPTIONS = new String[]{"128", "192", "256"};
    private static final String[] ENCODING_OPTIONS = new String[]{"Base64"};

    public static String getCipherInstanceName(String cipherAlgorithm, String cipherMode, String cipherPadding) {
        return cipherAlgorithm + '/' + cipherMode + '/' + cipherPadding;
    }

    public static Key getCipherKey(String password, String cipherAlgorithm, String symmetricBlockSize)
            throws NoSuchAlgorithmException, InvalidKeySpecException {
        byte[] byteKey;
        int len = password.length();
        byteKey = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            byteKey[i / 2] = (byte) ((Character.digit(password.charAt(i), 16) << 4)
                    + Character.digit(password.charAt(i + 1), 16));
        }
        return new SecretKeySpec(byteKey, cipherAlgorithm);

        /*
         PBEKeySpec keySpec = new PBEKeySpec(password.toCharArray(), getSalt(), KEY_SPEC_ITERATION,
         getSymmetricBlockSize(cipherAlgorithm, symmetricBlockSize));
         SecretKey secretKey = SecretKeyFactory.getInstance(SECRET_KEY_GENERATION_ALGORITHM).generateSecret(keySpec);

         return new SecretKeySpec(secretKey.getEncoded(), cipherAlgorithm);
         */
    }

    public static IvParameterSpec getInitialVector(String initializationVectorValuesStr) {
        if (StringUtils.isEmpty(initializationVectorValuesStr)) {
            return new IvParameterSpec(DEFAULT_INITIALIZATION_VECTOR);
        }
        String[] initializationVectorValues = initializationVectorValuesStr.split("\\s");
        byte[] byteValues = new byte[initializationVectorValues.length];
        for (int i = 0; i < initializationVectorValues.length; i++) {
            byteValues[i] = Byte.valueOf(initializationVectorValues[i]);
        }
        return new IvParameterSpec(byteValues);
    }

    public static String applyEncoding(byte[] input, String encoding) {
        if (BASE_64.equalsIgnoreCase(encoding)) {
            return new String(Base64.encodeBase64(input));
        }
        return new String(input);
    }

    public static byte[] applyDecoding(String input, String encoding) {
        if (BASE_64.equalsIgnoreCase(encoding)) {
            return Base64.decodeBase64(input);
        }
        return input.getBytes();
    }

    public static String replaceText(String xmlText, String oldText, String newText) {
        int start = xmlText.indexOf(oldText);
        int finish = start + oldText.length();
        xmlText = xmlText.substring(0, start) + newText + xmlText.substring(finish);
        return xmlText;
    }

    public static Cipher getCipher(int mode, Interceptor interceptor) throws NoSuchPaddingException,
            NoSuchAlgorithmException, InvalidAlgorithmParameterException, InvalidKeyException, BadPaddingException,
            IllegalBlockSizeException, InvalidKeySpecException {
        InterceptorParams params = interceptor.getParameters();
        Cipher cipher =
                Cipher.getInstance(CipherHelper.getCipherInstanceName(
                        (params == null) ? null : params.get(CipherHelper.CIPHER_ALGORITHM),
                        (params == null) ? null : params.get(CipherHelper.CIPHER_MODE),
                        (params == null) ? null : params.get(CipherHelper.CIPHER_PADDING)));
        Key cipherKey = CipherHelper.getCipherKey(
                (params == null) ? null : params.get(CipherHelper.PASSWORD),
                (params == null) ? null : params.get(CipherHelper.CIPHER_ALGORITHM),
                (params == null) ? null : params.get(CipherHelper.SYMMETRIC_BLOCK_SIZE));
        if (!"ECB".equals((params == null) ? null : params.get(CipherHelper.CIPHER_MODE))) {
            IvParameterSpec initialVector = CipherHelper.getInitialVector(
                    (params == null) ? null : params.get(CipherHelper.INITIALIZATION_VECTOR));
            cipher.init(mode, cipherKey, initialVector);
        } else {
            cipher.init(mode, cipherKey);
        }
        return cipher;
    }

    public static List<InterceptorPropertyDescriptor> getCipherParametersDefaultList(Interceptor interceptor) {
        List<InterceptorPropertyDescriptor> parameters = new ArrayList<>();
        InterceptorParams params = interceptor.getParameters();
        parameters.add(new InterceptorPropertyDescriptor(PASSWORD,
                PASSWORD,
                PASSWORD_DESCRIPTION,
                InterceptorConstants.TEXTFIELD,
                (params == null) ? null : params.get(PASSWORD),
                false));
        parameters.add(new InterceptorPropertyDescriptor(CIPHER_ALGORITHM,
                CIPHER_ALGORITHM,
                CIPHER_ALGORITHM_DESCRIPTION,
                InterceptorConstants.LIST,
                CIPHER_ALGORITHM_OPTIONS,
                (params == null) ? null : params.get(CIPHER_ALGORITHM),
                false));
        parameters.add(new InterceptorPropertyDescriptor(SYMMETRIC_BLOCK_SIZE,
                SYMMETRIC_BLOCK_SIZE,
                SYMMETRIC_BLOCK_SIZE_DESCRIPTION,
                InterceptorConstants.LIST,
                SYMMETRIC_BLOCKSIZE_OPTIONS,
                (params == null) ? null : params.get(SYMMETRIC_BLOCK_SIZE),
                false));
        parameters.add(new InterceptorPropertyDescriptor(CIPHER_MODE,
                CIPHER_MODE,
                CIPHER_MODE_DESCRIPTION,
                InterceptorConstants.LIST,
                CIPHER_MODE_OPTIONS,
                (params == null) ? null : params.get(CIPHER_MODE),
                false));
        parameters.add(new InterceptorPropertyDescriptor(CIPHER_PADDING,
                CIPHER_PADDING,
                CIPHER_PADDING_DESCRIPTION,
                InterceptorConstants.LIST,
                CIPHER_PADDING_OPTIONS,
                (params == null) ? null : params.get(CIPHER_PADDING),
                false));
        parameters.add(new InterceptorPropertyDescriptor(INITIALIZATION_VECTOR,
                INITIALIZATION_VECTOR,
                INITIALIZATION_VECTOR_DESCRIPTION,
                InterceptorConstants.TEXTFIELD,
                (params == null) ? null : params.get(INITIALIZATION_VECTOR),
                true));
        parameters.add(new InterceptorPropertyDescriptor(ENCODING,
                ENCODING,
                ENCODING_DESCRIPTION,
                InterceptorConstants.LIST,
                ENCODING_OPTIONS,
                (params == null) ? null : params.get(ENCODING),
                false));
        return parameters;
    }

    private static byte[] getSalt() {
        return new byte[]{0, 0, 0, 0, 0, 0, 0, 0};
    }

    private static int getSymmetricBlockSize(String cipherAlgorithm, String blockSizeStr) {
        if (AES.equals(cipherAlgorithm)) {
            return !StringUtils.isEmpty(blockSizeStr) ? Integer.parseInt(blockSizeStr) : DEFAULT_AES_BLOCK_SIZE;
        } else if (DES.equals(cipherAlgorithm)) {
            return DES_BLOCK_SIZE;
        } else {
            return DESEDE_BLOCK_SIZE;
        }
    }
}

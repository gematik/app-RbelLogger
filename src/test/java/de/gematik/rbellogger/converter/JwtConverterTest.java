/*
 * Copyright (c) 2021 gematik GmbH
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *    http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package de.gematik.rbellogger.converter;

import static org.assertj.core.api.Assertions.assertThat;

import de.gematik.rbellogger.RbelLogger;
import de.gematik.rbellogger.data.*;
import java.io.File;
import java.io.IOException;
import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.Test;

public class JwtConverterTest {

    final static String JWT = "eyJhbGciOiJCUDI1NlIxIiwia2lkIjoiZGlzY1NpZyIsIng1YyI6WyJNSUlDc1RDQ0FsaWdBd0lCQWdJSEFic3NxUWhxT3pBS0JnZ3Foa2pPUFFRREFqQ0JoREVMTUFrR0ExVUVCaE1DUkVVeEh6QWRCZ05WQkFvTUZtZGxiV0YwYVdzZ1IyMWlTQ0JPVDFRdFZrRk1TVVF4TWpBd0JnTlZCQXNNS1V0dmJYQnZibVZ1ZEdWdUxVTkJJR1JsY2lCVVpXeGxiV0YwYVd0cGJtWnlZWE4wY25WcmRIVnlNU0F3SGdZRFZRUUREQmRIUlUwdVMwOU5VQzFEUVRFd0lGUkZVMVF0VDA1TVdUQWVGdzB5TVRBeE1UVXdNREF3TURCYUZ3MHlOakF4TVRVeU16VTVOVGxhTUVreEN6QUpCZ05WQkFZVEFrUkZNU1l3SkFZRFZRUUtEQjFuWlcxaGRHbHJJRlJGVTFRdFQwNU1XU0F0SUU1UFZDMVdRVXhKUkRFU01CQUdBMVVFQXd3SlNVUlFJRk5wWnlBek1Gb3dGQVlIS29aSXpqMENBUVlKS3lRREF3SUlBUUVIQTBJQUJJWVpud2lHQW41UVlPeDQzWjhNd2FaTEQzci9iejZCVGNRTzVwYmV1bTZxUXpZRDVkRENjcml3L1ZOUFBaQ1F6WFFQZzRTdFd5eTVPT3E5VG9nQkVtT2pnZTB3Z2Vvd0RnWURWUjBQQVFIL0JBUURBZ2VBTUMwR0JTc2tDQU1EQkNRd0lqQWdNQjR3SERBYU1Bd01Da2xFVUMxRWFXVnVjM1F3Q2dZSUtvSVVBRXdFZ2dRd0lRWURWUjBnQkJvd0dEQUtCZ2dxZ2hRQVRBU0JTekFLQmdncWdoUUFUQVNCSXpBZkJnTlZIU01FR0RBV2dCUW84UGptcWNoM3pFTkYyNXF1MXpxRHJBNFBxREE0QmdnckJnRUZCUWNCQVFRc01Db3dLQVlJS3dZQkJRVUhNQUdHSEdoMGRIQTZMeTlsYUdOaExtZGxiV0YwYVdzdVpHVXZiMk56Y0M4d0hRWURWUjBPQkJZRUZDOTRNOUxnVzQ0bE5nb0Fia1Bhb21uTGpTOC9NQXdHQTFVZEV3RUIvd1FDTUFBd0NnWUlLb1pJemowRUF3SURSd0F3UkFJZ0NnNHlaRFdteUJpcmd4emF3ei9TOERKblJGS3RZVS9ZR05sUmM3K2tCSGNDSUJ1emJhM0dzcHFTbW9QMVZ3TWVOTktOYUxzZ1Y4dk1iREpiMzBhcWFpWDEiXX0.eyJhdXRob3JpemF0aW9uX2VuZHBvaW50IjoiaHR0cDovL2xvY2FsaG9zdDo4MDgwL3NpZ25fcmVzcG9uc2UiLCJhbHRlcm5hdGl2ZV9hdXRob3JpemF0aW9uX2VuZHBvaW50IjoiaHR0cDovL2xvY2FsaG9zdDo4MDgwL2FsdF9yZXNwb25zZSIsInNzb19lbmRwb2ludCI6Imh0dHA6Ly9sb2NhbGhvc3Q6ODA4MC9zc29fcmVzcG9uc2UiLCJwYWlyaW5nX2VuZHBvaW50IjoiaHR0cDovL2xvY2FsaG9zdDo4MDgwL3BhaXJpbmciLCJ0b2tlbl9lbmRwb2ludCI6Imh0dHA6Ly9sb2NhbGhvc3Q6ODA4MC90b2tlbiIsInVyaV9kaXNjIjoiaHR0cDovL2xvY2FsaG9zdDo4MDgwL2Rpc2NvdmVyeURvY3VtZW50IiwiaXNzdWVyIjoiaHR0cHM6Ly9pZHAuemVudHJhbC5pZHAuc3BsaXRkbnMudGktZGllbnN0ZS5kZSIsImp3a3NfdXJpIjoiaHR0cDovL2xvY2FsaG9zdDo4MDgwL2p3a3MiLCJleHAiOjE2MTQ0MjIwMzQsIm5iZiI6MTYxNDMzNTYzNCwiaWF0IjoxNjE0MzM1NjM0LCJ1cmlfcHVrX2lkcF9lbmMiOiJodHRwOi8vbG9jYWxob3N0OjgwODAvaWRwRW5jL2p3a3MuanNvbiIsInVyaV9wdWtfaWRwX3NpZyI6Imh0dHA6Ly9sb2NhbGhvc3Q6ODA4MC9pcGRTaWcvandrcy5qc29uIiwic3ViamVjdF90eXBlc19zdXBwb3J0ZWQiOlsicGFpcndpc2UiXSwiaWRfdG9rZW5fc2lnbmluZ19hbGdfdmFsdWVzX3N1cHBvcnRlZCI6WyJCUDI1NlIxIl0sInJlc3BvbnNlX3R5cGVzX3N1cHBvcnRlZCI6WyJjb2RlIl0sInNjb3Blc19zdXBwb3J0ZWQiOlsib3BlbmlkIiwiZS1yZXplcHQiXSwicmVzcG9uc2VfbW9kZXNfc3VwcG9ydGVkIjpbInF1ZXJ5Il0sImdyYW50X3R5cGVzX3N1cHBvcnRlZCI6WyJhdXRob3JpemF0aW9uX2NvZGUiXSwiYWNyX3ZhbHVlc19zdXBwb3J0ZWQiOlsidXJuOmVpZGFzOmxvYTpoaWdoIl0sInRva2VuX2VuZHBvaW50X2F1dGhfbWV0aG9kc19zdXBwb3J0ZWQiOlsibm9uZSJdfQ.AB892jPGe-4aOn5wEW9ManAD_AxmNvmfulNt20dbAQUqkIBdu3WuzMzOkodh4nRKtRxklht21E86_GZsZl_I-Q";

    @Test
    public void convertMessage_shouldGiveJwtBody() throws IOException {
        final String curlMessage = FileUtils
            .readFileToString(new File("src/test/resources/sampleMessages/jwtMessage.curl"));

        final RbelElement convertedMessage = RbelLogger.build().getRbelConverter().convertMessage(curlMessage);

        assertThat(((RbelHttpResponse) convertedMessage).getBody())
            .isInstanceOf(RbelJwtElement.class);
    }

    @Test
    public void convertMessage_shouldGiveJsonBody() {
        final RbelElement convertedMessage = new RbelJwtConverter()
            .convertElement(new RbelStringElement(JWT), RbelLogger.build().getRbelConverter());

        assertThat(convertedMessage)
            .isInstanceOf(RbelJwtElement.class);
    }

    @Test
    public void convertMessage_shouldContainValidSignature() {
        final RbelElement convertedMessage = new RbelJwtConverter()
            .convertElement(new RbelStringElement(JWT), RbelLogger.build().getRbelConverter());

        assertThat(((RbelJwtElement) convertedMessage).getSignature()).isNotNull();
        assertThat(((RbelJwtElement) convertedMessage).getSignature().isValid()).isTrue();
    }

    @Test
    public void convertMessage_shouldContainInvalidSignature() {
        final RbelElement convertedMessage = new RbelJwtConverter()
            .convertElement(new RbelStringElement(JWT + "sigInvalid"), RbelLogger.build().getRbelConverter());

        assertThat(((RbelJwtElement) convertedMessage).getSignature()).isNotNull();
        assertThat(((RbelJwtElement) convertedMessage).getSignature().isValid()).isFalse();
    }

    @Test
    public void keyMessageAndJwtReply_shouldValidateSignature() throws IOException {
        final String keyMessage = FileUtils
            .readFileToString(new File("src/test/resources/sampleMessages/idpSigMessage.curl"));
        final String challengeMessage = FileUtils
            .readFileToString(new File("src/test/resources/sampleMessages/getChallenge.curl"));
        final RbelConverter rbelConverter = RbelLogger.build().getRbelConverter();

        rbelConverter.convertMessage(keyMessage);
        final RbelElement convertedMessage = rbelConverter.convertMessage(challengeMessage);
        final RbelJwtElement challenge = ((RbelJwtElement) ((RbelMapElement) ((RbelJsonElement) ((RbelHttpResponse) convertedMessage)
            .getBody()).getJsonElement()).getElementMap()
            .get("challenge"));

        assertThat(challenge.getSignature()).isNotNull();
        assertThat(challenge.getSignature().isValid()).isTrue();
        assertThat(challenge.getSignature().getVerifiedUsing()).isEqualTo("idpSig");
    }
}

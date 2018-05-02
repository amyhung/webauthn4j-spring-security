/*
 * Copyright 2002-2018 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.webauthn4j.jackson.deserializer;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.fasterxml.jackson.dataformat.cbor.CBORFactory;
import com.webauthn4j.attestation.authenticator.AbstractCredentialPublicKey;
import com.webauthn4j.attestation.authenticator.AttestedCredentialData;
import com.webauthn4j.attestation.authenticator.AuthenticatorData;
import com.webauthn4j.attestation.authenticator.ESCredentialPublicKey;
import com.webauthn4j.attestation.authenticator.extension.Extension;
import com.webauthn4j.util.UnsignedNumberUtil;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

/**
 * Jackson Deserializer for AuthenticatorData
 */
public class AuthenticatorDataDeserializer extends StdDeserializer<AuthenticatorData> {

    private ObjectMapper objectMapper = new ObjectMapper(new CBORFactory());

    //TODO: make protected
    public AuthenticatorDataDeserializer() {
        super(AuthenticatorData.class);
    }

    @Override
    public AuthenticatorData deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        byte[] value = p.getBinaryValue();
        return deserialize(value);
    }

    public AuthenticatorData deserialize(byte[] value) {
        ByteBuffer byteBuffer = ByteBuffer.wrap(value);
        AuthenticatorData authenticatorData = new AuthenticatorData();

        byte[] rpIdHash = new byte[32];
        byteBuffer.get(rpIdHash, 0, 32);
        byte flags = byteBuffer.get();
        long counter = UnsignedNumberUtil.getUnsignedInt(byteBuffer);

        authenticatorData.setRpIdHash(rpIdHash);
        authenticatorData.setFlags(flags);
        authenticatorData.setCounter(counter);

        AttestedCredentialData attestationData;
        List<Extension> extensions;
        if (authenticatorData.isFlagAT()) {
            attestationData = deserializeAttestedCredentialData(byteBuffer);
        } else {
            attestationData = null;
        }
        if (authenticatorData.isFlagED()) {
            extensions = deserializeExtensions(byteBuffer);
        } else {
            extensions = null;
        }

        authenticatorData.setAttestedCredentialData(attestationData);
        authenticatorData.setExtensions(extensions);

        return authenticatorData;
    }

    AttestedCredentialData deserializeAttestedCredentialData(ByteBuffer byteBuffer) {
        byte[] aaGuid = new byte[16];
        byteBuffer.get(aaGuid, 0, 16);
        int length = UnsignedNumberUtil.getUnsignedShort(byteBuffer);
        byte[] credentialId = new byte[length];
        byteBuffer.get(credentialId, 0, length);
        byte[] remaining = new byte[byteBuffer.remaining()];
        byteBuffer.get(remaining);
        ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(remaining);
        AbstractCredentialPublicKey credentialPublicKey = deserializeCredentialPublicKey(byteArrayInputStream);
        AttestedCredentialData attestationData = new AttestedCredentialData(aaGuid, credentialId, credentialPublicKey);
        int extensionsBufferLength = byteArrayInputStream.available();
        byteBuffer.position(byteBuffer.position() - extensionsBufferLength);
        return attestationData;
    }

    AbstractCredentialPublicKey deserializeCredentialPublicKey(InputStream inputStream) {
        try {
            return objectMapper.readValue(inputStream, ESCredentialPublicKey.class); //TODO
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    List<Extension> deserializeExtensions(ByteBuffer byteBuffer) {
        if (byteBuffer.remaining() == 0) {
            return new ArrayList<>();
        }
        byte[] remaining = new byte[byteBuffer.remaining()];
        byteBuffer.get(remaining);
        try {
            return objectMapper.readValue(remaining, new TypeReference<List<Extension>>() {
            });
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

}

/*
 * Copyright 2018 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.webauthn4j.springframework.security.webauthn.sample.exp;

import com.webauthn4j.data.attestation.statement.AttestationType;
import com.webauthn4j.util.MessageDigestUtil;
import com.webauthn4j.validator.RegistrationObject;
import com.webauthn4j.validator.attestation.statement.AbstractStatementValidator;
import com.webauthn4j.validator.exception.BadAttestationStatementException;
import com.webauthn4j.validator.exception.PublicKeyMismatchException;
import org.apache.kerby.asn1.parse.Asn1Container;
import org.apache.kerby.asn1.parse.Asn1ParseResult;
import org.apache.kerby.asn1.parse.Asn1Parser;
import org.apache.kerby.asn1.type.Asn1OctetString;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.security.MessageDigest;
import java.security.PublicKey;
import java.util.Arrays;

public class AppleAnonymousAttestationStatementValidator extends AbstractStatementValidator<AppleAnonymousAttestationStatement> {

    // ~ Instance fields
    // ================================================================================================


    @Override
    public AttestationType validate(RegistrationObject registrationObject) {
        if (!supports(registrationObject)) {
            throw new IllegalArgumentException(String.format("Specified format '%s' is not supported by %s.", registrationObject.getAttestationObject().getFormat(), this.getClass().getName()));
        }

        AppleAnonymousAttestationStatement attestationStatement =
                (AppleAnonymousAttestationStatement) registrationObject.getAttestationObject().getAttestationStatement();

        if (attestationStatement.getX5c() == null || attestationStatement.getX5c().isEmpty()) {
            throw new BadAttestationStatementException("No attestation certificate is found in apple anonymous attestation statement.");
        }

        validateNonce(registrationObject);

        PublicKey publicKeyInEndEntityCert = attestationStatement.getX5c().getEndEntityAttestationCertificate().getCertificate().getPublicKey();
        PublicKey publicKeyInCredentialData = registrationObject.getAttestationObject().getAuthenticatorData().getAttestedCredentialData().getCOSEKey().getPublicKey();
        if (!publicKeyInEndEntityCert.equals(publicKeyInCredentialData)) {
            throw new PublicKeyMismatchException("The public key in the first certificate in x5c doesn't matches the credentialPublicKey in the attestedCredentialData in authenticatorData.");
        }

        return AttestationType.BASIC;
    }

    private void validateNonce(RegistrationObject registrationObject) {
        AppleAnonymousAttestationStatement attestationStatement = (AppleAnonymousAttestationStatement) registrationObject.getAttestationObject().getAttestationStatement();

        byte[] nonce = getNonce(registrationObject);
        byte[] extensionValue = attestationStatement.getX5c().getEndEntityAttestationCertificate().getCertificate().getExtensionValue("1.2.840.113635.100.8.2");
        byte[] extracted;
        try {
            Asn1OctetString extensionEnvelope = new Asn1OctetString();
            extensionEnvelope.decode(extensionValue);
            extensionEnvelope.getValue();
            byte[] extensionEnvelopeValue = extensionEnvelope.getValue();
            Asn1Container container = (Asn1Container) Asn1Parser.parse(ByteBuffer.wrap(extensionEnvelopeValue));
            Asn1ParseResult firstElement = container.getChildren().get(0);
            Asn1OctetString octetString = new Asn1OctetString();
            octetString.decode(firstElement);
            extracted = octetString.getValue();
        }
        catch (IOException | RuntimeException e) {
            throw new BadAttestationStatementException("Failed to extract nonce from Apple anonymous attestation statement.", e);
        }
        if(!Arrays.equals(extracted, nonce)){
            throw new BadAttestationStatementException("nonce doesn't match.");
        }
    }

    private byte[] getNonce(RegistrationObject registrationObject) {
        MessageDigest messageDigest = MessageDigestUtil.createSHA256();
        byte[] authenticatorData = registrationObject.getAuthenticatorDataBytes();
        byte[] clientDataHash = messageDigest.digest(registrationObject.getCollectedClientDataBytes());
        byte[] data = ByteBuffer.allocate(authenticatorData.length + clientDataHash.length).put(authenticatorData).put(clientDataHash).array();
        return MessageDigestUtil.createSHA256().digest(data);
    }

}

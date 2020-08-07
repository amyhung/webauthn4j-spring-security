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

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.webauthn4j.data.attestation.statement.AttestationCertificatePath;
import com.webauthn4j.data.attestation.statement.COSEAlgorithmIdentifier;
import com.webauthn4j.data.attestation.statement.CertificateBaseAttestationStatement;
import com.webauthn4j.validator.exception.ConstraintViolationException;

import java.util.Objects;

@JsonIgnoreProperties(value = "format")
@JsonTypeName(AppleAnonymousAttestationStatement.FORMAT)
public class AppleAnonymousAttestationStatement implements CertificateBaseAttestationStatement {

    public static final String FORMAT = "apple";

    @JsonProperty
    private final COSEAlgorithmIdentifier alg;

    @JsonProperty
    private final AttestationCertificatePath x5c;

    public AppleAnonymousAttestationStatement(
            @JsonProperty("alg") COSEAlgorithmIdentifier alg,
            @JsonProperty("x5c") AttestationCertificatePath x5c) {
        this.alg = alg;
        this.x5c = x5c;
    }

    public COSEAlgorithmIdentifier getAlg() {
        return alg;
    }

    @Override
    public AttestationCertificatePath getX5c() {
        return x5c;
    }

    @JsonIgnore
    @Override
    public String getFormat() {
        return FORMAT;
    }

    @Override
    public void validate() {
        if (alg == null) {
            throw new ConstraintViolationException("alg must not be null");
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AppleAnonymousAttestationStatement that = (AppleAnonymousAttestationStatement) o;
        return Objects.equals(alg, that.alg) &&
                Objects.equals(x5c, that.x5c);
    }

    @Override
    public int hashCode() {
        return Objects.hash(alg, x5c);
    }
}

/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.licence;

import com.bytechef.platform.licence.Licence;
import com.bytechef.platform.licence.LicenceException;
import com.bytechef.platform.licence.LicenceFeature;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.EnumSet;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

/**
 * Parses and verifies a Keygen signed license file (alg base64+ed25519) fully offline.
 *
 * @version ee
 */
public class LicenceFileParser {

    private static final Logger log = LoggerFactory.getLogger(LicenceFileParser.class);

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private final Ed25519Verifier verifier;

    public LicenceFileParser(Ed25519Verifier verifier) {
        this.verifier = verifier;
    }

    public Licence parse(byte[] licenceFileBytes) {
        String armored = new String(licenceFileBytes, StandardCharsets.UTF_8);

        String envelopeBase64 = armored
            .replace("-----BEGIN LICENSE FILE-----", "")
            .replace("-----END LICENSE FILE-----", "")
            .replaceAll("\\s", "");

        if (envelopeBase64.isBlank()) {
            throw new LicenceException("Empty or malformed licence file");
        }

        try {
            byte[] envelopeBytes = Base64.getDecoder()
                .decode(envelopeBase64);

            JsonNode envelope = OBJECT_MAPPER.readTree(envelopeBytes);

            String enc = textField(envelope, "enc");
            String sig = textField(envelope, "sig");

            if (enc == null || sig == null) {
                throw new LicenceException("Licence file missing enc/sig");
            }

            byte[] signedBytes = ("license/" + enc).getBytes(StandardCharsets.UTF_8);
            byte[] signature = Base64.getDecoder()
                .decode(sig);

            if (!verifier.verify(signedBytes, signature)) {
                throw new LicenceException("Licence signature verification failed");
            }

            byte[] datasetBytes = Base64.getDecoder()
                .decode(enc);

            return toLicence(OBJECT_MAPPER.readTree(datasetBytes));
        } catch (LicenceException licenceException) {
            throw licenceException;
        } catch (Exception exception) {
            throw new LicenceException("Failed to parse licence file", exception);
        }
    }

    private static Licence toLicence(JsonNode root) {
        JsonNode attributes = root.path("data")
            .path("attributes");
        JsonNode metadata = attributes.path("metadata");

        String id = textField(root.path("data"), "id");
        Instant expiresAt = parseInstant(textField(attributes, "expiry"));
        Instant issuedAt = parseInstant(textField(attributes, "created"));

        long allowedJobs = parseAllowedJobs(metadata.get("allowedJobs"));
        Set<LicenceFeature> features = parseFeatures(metadata.get("features"));

        JsonNode maxUsersNode = metadata.get("maxUsers");
        Integer maxUsers = (maxUsersNode != null && !maxUsersNode.isNull() && maxUsersNode.isNumber())
            ? maxUsersNode.asInt() : null;

        return new Licence(
            id, textField(metadata, "holderName"), textField(metadata, "holderEmail"), issuedAt, expiresAt, features,
            allowedJobs, maxUsers);
    }

    private static Set<LicenceFeature> parseFeatures(JsonNode featuresNode) {
        Set<LicenceFeature> features = EnumSet.noneOf(LicenceFeature.class);

        if (featuresNode != null && featuresNode.isArray()) {
            for (JsonNode featureNode : featuresNode) {
                String key = featureNode.asString();

                LicenceFeature.ofKey(key)
                    .ifPresentOrElse(
                        features::add,
                        () -> log.warn("Unknown licence feature key '{}'; ignoring", key));
            }
        }

        return features;
    }

    private static long parseAllowedJobs(JsonNode node) {
        if (node == null || node.isNull()) {
            return -1;
        }

        if (node.isNumber()) {
            return node.asLong();
        }

        try {
            return Long.parseLong(node.asString()
                .trim());
        } catch (NumberFormatException exception) {
            return -1;
        }
    }

    private static Instant parseInstant(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }

        return Instant.parse(value);
    }

    private static String textField(JsonNode node, String field) {
        JsonNode value = node.get(field);

        return (value == null || value.isNull()) ? null : value.asString();
    }
}

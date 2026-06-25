/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.licence;

import com.bytechef.ee.platform.licence.domain.LicenceEntity;
import com.bytechef.ee.platform.licence.repository.LicenceRepository;
import com.bytechef.platform.licence.Licence;
import com.bytechef.platform.licence.LicenceException;
import com.bytechef.platform.licence.LicenceFeature;
import com.bytechef.platform.licence.LicenceManager;
import com.bytechef.platform.licence.LicenceStatus;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Iterator;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * EE offline implementation of {@link LicenceManager} that verifies licence files using Ed25519 signatures and persists
 * a single licence row in the database. Grace logic: after the licence expiry date, the licence remains
 * {@link LicenceStatus#GRACE active} for {@code gracePeriodDays} days before transitioning to
 * {@link LicenceStatus#EXPIRED}.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
public class OfflineLicenceManager implements LicenceManager {

    private static final Logger log = LoggerFactory.getLogger(OfflineLicenceManager.class);

    private final LicenceFileParser parser;
    private final LicenceRepository licenceRepository;
    private final Clock clock;
    private final int gracePeriodDays;

    private volatile Licence licence;
    private volatile boolean invalid;

    public OfflineLicenceManager(
        LicenceFileParser parser, LicenceRepository licenceRepository, Clock clock, int gracePeriodDays) {

        this.parser = parser;
        this.licenceRepository = licenceRepository;
        this.clock = clock;
        this.gracePeriodDays = gracePeriodDays;

        loadFromRepository();
    }

    @Override
    public LicenceStatus getStatus() {
        if (invalid) {
            return LicenceStatus.INVALID;
        }

        Licence current = licence;

        if (current == null) {
            return LicenceStatus.MISSING;
        }

        Instant expiresAt = current.expiresAt();

        if (expiresAt == null) {
            return LicenceStatus.VALID;
        }

        Instant now = Instant.now(clock);

        if (!now.isAfter(expiresAt)) {
            return LicenceStatus.VALID;
        }

        Instant graceEnd = expiresAt.plus(gracePeriodDays, ChronoUnit.DAYS);

        if (!now.isAfter(graceEnd)) {
            return LicenceStatus.GRACE;
        }

        return LicenceStatus.EXPIRED;
    }

    @Override
    public Optional<Licence> getLicence() {
        return Optional.ofNullable(licence);
    }

    @Override
    public boolean isFeatureEnabled(LicenceFeature licenceFeature) {
        Licence current = licence;

        return getStatus().isActive() && current != null && current.features()
            .contains(licenceFeature);
    }

    @Override
    public void checkFeature(LicenceFeature licenceFeature) {
        if (!isFeatureEnabled(licenceFeature)) {
            throw new LicenceException(
                "Feature %s is not enabled by the current licence (status=%s)".formatted(
                    licenceFeature.getKey(), getStatus()));
        }
    }

    @Override
    public long getAllowedJobs() {
        Licence current = licence;

        if (current == null || !getStatus().isActive()) {
            return -1;
        }

        return current.allowedJobs();
    }

    @Override
    public Licence upload(byte[] licenceFileBytes) {
        Licence parsed = parser.parse(licenceFileBytes);

        String rawFile = new String(licenceFileBytes, StandardCharsets.UTF_8);

        licenceRepository.deleteAll();

        LicenceEntity entity = new LicenceEntity();
        entity.setRawFile(rawFile);

        licenceRepository.save(entity);

        this.licence = parsed;
        this.invalid = false;

        return parsed;
    }

    @Override
    public void delete() {
        licenceRepository.deleteAll();

        this.licence = null;
        this.invalid = false;
    }

    /**
     * Seeds the database from a bootstrap source if no licence is currently active.
     *
     * <p>
     * If a licence is already cached in memory or the repository already contains a row, this method is a no-op.
     * Otherwise, bytes are read from {@code inlineContents} (preferred) or from the file at {@code path}, and
     * {@link #upload(byte[])} is called. Any {@link LicenceException} or {@link IOException} is logged as a warning so
     * the application always starts cleanly.
     *
     * @param path           filesystem path to a {@code .lic} file, or {@code null}
     * @param inlineContents UTF-8 licence file contents (e.g. from an env var), or {@code null}
     */
    public void bootstrap(String path, String inlineContents) {
        if (licence != null || licenceRepository.count() > 0) {
            return;
        }

        byte[] bytes = null;

        if (inlineContents != null && !inlineContents.isBlank()) {
            bytes = inlineContents.getBytes(StandardCharsets.UTF_8);
        } else if (path != null && !path.isBlank()) {
            try {
                bytes = Files.readAllBytes(Path.of(path));
            } catch (IOException ioException) {
                log.warn("Failed to read bootstrap licence file at {}: {}", path, ioException.getMessage());

                return;
            }
        }

        if (bytes != null) {
            try {
                upload(bytes);
            } catch (RuntimeException exception) {
                log.warn("Bootstrap licence failed: {}", exception.getMessage());
            }
        }
    }

    /**
     * Marks the cached licence as cryptographically invalid. Called by a later check-in task when a server-side
     * validation fails.
     */
    void markInvalid() {
        this.invalid = true;
    }

    /**
     * Clears the invalid flag when a subsequent check-in confirms the licence is actively valid. A no-op when no
     * licence is cached.
     */
    void clearInvalid() {
        this.invalid = false;
    }

    private void loadFromRepository() {
        Iterator<LicenceEntity> iterator = licenceRepository.findAll()
            .iterator();

        if (!iterator.hasNext()) {
            return;
        }

        LicenceEntity entity = iterator.next();

        try {
            this.licence = parser.parse(entity.getRawFile()
                .getBytes(StandardCharsets.UTF_8));
        } catch (LicenceException licenceException) {
            log.warn("Persisted licence failed verification on startup: {}", licenceException.getMessage());

            this.invalid = true;
        }
    }
}

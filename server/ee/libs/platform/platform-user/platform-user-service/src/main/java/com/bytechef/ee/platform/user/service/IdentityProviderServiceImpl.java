/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.user.service;

import com.bytechef.ee.platform.user.domain.IdentityProvider;
import com.bytechef.ee.platform.user.event.IdentityProviderChangedEvent;
import com.bytechef.ee.platform.user.repository.IdentityProviderRepository;
import com.bytechef.encryption.Encryption;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import java.util.List;
import java.util.Optional;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@Service
@Transactional
@ConditionalOnEEVersion
public class IdentityProviderServiceImpl implements IdentityProviderService {

    private final ApplicationEventPublisher applicationEventPublisher;
    private final Encryption encryption;
    private final IdentityProviderRepository identityProviderRepository;

    public IdentityProviderServiceImpl(
        ApplicationEventPublisher applicationEventPublisher, Encryption encryption,
        IdentityProviderRepository identityProviderRepository) {

        this.applicationEventPublisher = applicationEventPublisher;
        this.encryption = encryption;
        this.identityProviderRepository = identityProviderRepository;
    }

    @Override
    public IdentityProvider create(IdentityProvider identityProvider) {
        identityProvider.setClientSecret(encryption.encrypt(identityProvider.getClientSecret()));

        IdentityProvider savedIdentityProvider = identityProviderRepository.save(identityProvider);

        applicationEventPublisher.publishEvent(new IdentityProviderChangedEvent());

        return savedIdentityProvider;
    }

    @Override
    public void delete(long id) {
        identityProviderRepository.deleteById(id);

        applicationEventPublisher.publishEvent(new IdentityProviderChangedEvent());
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<IdentityProvider> fetchByDomain(String emailDomain) {
        return identityProviderRepository.findByDomain(emailDomain.toLowerCase());
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<IdentityProvider> fetchByName(String name) {
        return identityProviderRepository.findByNameIgnoreCase(name);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<IdentityProvider> fetchByScimApiKey(String scimApiKey) {
        return identityProviderRepository.findByScimApiKey(scimApiKey);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<IdentityProvider> fetchMcpIdentityProvider() {
        return identityProviderRepository.findMcpIdentityProvider();
    }

    @Override
    @Transactional(readOnly = true)
    public IdentityProvider getIdentityProvider(long id) {
        return identityProviderRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Identity provider not found: " + id));
    }

    @Override
    @Transactional(readOnly = true)
    public List<IdentityProvider> getIdentityProviders() {
        return identityProviderRepository.findAll();
    }

    @Override
    public IdentityProvider update(IdentityProvider identityProvider) {
        IdentityProvider existingIdentityProvider = getIdentityProvider(identityProvider.getId());

        String clientSecret = identityProvider.getClientSecret();

        if (clientSecret != null && !clientSecret.isEmpty()) {
            identityProvider.setClientSecret(encryption.encrypt(clientSecret));
        } else {
            identityProvider.setClientSecret(existingIdentityProvider.getClientSecret());
        }

        IdentityProvider savedIdentityProvider = identityProviderRepository.save(identityProvider);

        applicationEventPublisher.publishEvent(new IdentityProviderChangedEvent());

        return savedIdentityProvider;
    }

    @Override
    @Transactional(readOnly = true)
    public String getDecryptedClientSecret(IdentityProvider identityProvider) {
        return encryption.decrypt(identityProvider.getClientSecret());
    }
}

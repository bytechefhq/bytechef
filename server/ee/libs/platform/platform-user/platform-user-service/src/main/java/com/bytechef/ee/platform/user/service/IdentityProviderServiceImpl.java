/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.user.service;

import com.bytechef.ee.platform.user.domain.IdentityProvider;
import com.bytechef.ee.platform.user.domain.ScimApiKey;
import com.bytechef.ee.platform.user.repository.IdentityProviderRepository;
import com.bytechef.encryption.Encryption;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import com.bytechef.tenant.TenantContext;
import java.util.List;
import java.util.Optional;
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

    private final Encryption encryption;
    private final IdentityProviderRepository identityProviderRepository;

    public IdentityProviderServiceImpl(
        Encryption encryption, IdentityProviderRepository identityProviderRepository) {

        this.encryption = encryption;
        this.identityProviderRepository = identityProviderRepository;
    }

    @Override
    public IdentityProvider create(IdentityProvider identityProvider) {
        String clientSecret = identityProvider.getClientSecret();

        if (clientSecret != null && !clientSecret.isEmpty()) {
            identityProvider.setClientSecret(encryption.encrypt(clientSecret));
        }

        return identityProviderRepository.save(identityProvider);
    }

    @Override
    public IdentityProvider createServiceProviderSigningCredentialIfAbsent(
        long id, String certificate, String privateKey) {

        identityProviderRepository.updateServiceProviderSigningCredentialIfAbsent(
            id, certificate, encryption.encrypt(privateKey));

        return getIdentityProvider(id);
    }

    @Override
    public void delete(long id) {
        identityProviderRepository.deleteById(id);
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
        return identityProviderRepository.findByScimApiKey(ScimApiKey.hash(scimApiKey));
    }

    @Override
    public String generateScimApiKey(long id) {
        IdentityProvider identityProvider = getIdentityProvider(id);

        String scimApiKey = ScimApiKey.generate(TenantContext.getCurrentTenantId());

        identityProvider.setScimApiKey(ScimApiKey.hash(scimApiKey));

        identityProviderRepository.save(identityProvider);

        return scimApiKey;
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

        identityProvider.setCreatedBy(existingIdentityProvider.getCreatedBy());
        identityProvider.setCreatedDate(existingIdentityProvider.getCreatedDate());
        identityProvider.setScimApiKey(existingIdentityProvider.getScimApiKey());
        identityProvider.setServiceProviderSigningCertificate(
            existingIdentityProvider.getServiceProviderSigningCertificate());
        identityProvider.setServiceProviderSigningKey(existingIdentityProvider.getServiceProviderSigningKey());

        return identityProviderRepository.save(identityProvider);
    }

    @Override
    @Transactional(readOnly = true)
    public String getDecryptedClientSecret(IdentityProvider identityProvider) {
        return encryption.decrypt(identityProvider.getClientSecret());
    }

    @Override
    @Transactional(readOnly = true)
    public String getDecryptedServiceProviderSigningKey(IdentityProvider identityProvider) {
        return encryption.decrypt(identityProvider.getServiceProviderSigningKey());
    }
}

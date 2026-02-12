/*
 * Copyright 2025 ByteChef
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

package com.bytechef.platform.user.service;

import com.bytechef.encryption.Encryption;
import com.bytechef.platform.user.domain.IdentityProvider;
import com.bytechef.platform.user.repository.IdentityProviderRepository;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author Ivica Cardic
 */
@Service
@Transactional
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
        identityProvider.setClientSecret(encryption.encrypt(identityProvider.getClientSecret()));

        return identityProviderRepository.save(identityProvider);
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

        return identityProviderRepository.save(identityProvider);
    }

    @Override
    @Transactional(readOnly = true)
    public String getDecryptedClientSecret(IdentityProvider identityProvider) {
        return encryption.decrypt(identityProvider.getClientSecret());
    }
}

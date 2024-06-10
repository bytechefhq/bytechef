/*
 * Copyright 2023-present ByteChef Inc.
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

import com.bytechef.platform.user.domain.PersistentToken;
import com.bytechef.platform.user.repository.PersistentTokenRepository;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author Ivica Cardic
 */
@Service
@Transactional
public class PersistentTokenServiceImpl implements PersistentTokenService {

    private final PersistentTokenRepository persistentTokenRepository;

    public PersistentTokenServiceImpl(PersistentTokenRepository persistentTokenRepository) {
        this.persistentTokenRepository = persistentTokenRepository;
    }

    @Override
    public void delete(String id) {
        persistentTokenRepository.deleteById(id);
    }

    @Override
    public Optional<PersistentToken> fetchPersistentToken(String id) {
        return persistentTokenRepository.findById(id);
    }

    @Override
    public List<PersistentToken> getUserPersistentTokens(long userId) {
        return persistentTokenRepository.findAllByUserId(userId);
    }

    @Override
    public PersistentToken save(PersistentToken token) {
        return persistentTokenRepository.save(token);
    }
}

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

package com.bytechef.embedded.configuration.service;

import com.bytechef.embedded.configuration.domain.ConnectedUser;
import com.bytechef.embedded.configuration.repository.ConnectedUserRepository;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.time.LocalDate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author Ivica Cardic
 */
@Service
@Transactional
public class ConnectedUserServiceImpl implements ConnectedUserService {

    private final ConnectedUserRepository connectedUserRepository;

    @SuppressFBWarnings("EI")
    public ConnectedUserServiceImpl(ConnectedUserRepository connectedUserRepository) {
        this.connectedUserRepository = connectedUserRepository;
    }

    @Override
    public void deleteConnectedUser(long id) {
        connectedUserRepository.deleteById(id);
    }

    @Override
    public void enableConnectedUser(long id, boolean enable) {
        ConnectedUser connectedUser = getConnectedUser(id);

        connectedUser.setEnabled(enable);

        connectedUserRepository.save(connectedUser);
    }

    @Override
    @Transactional(readOnly = true)
    public ConnectedUser getConnectedUser(long id) {
        return connectedUserRepository.findById(id)
            .orElseThrow();
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ConnectedUser> getConnectedUsers(
        String search, LocalDate createDateFrom, LocalDate createDateTo, Long integrationId, int pageNumber) {

        PageRequest pageRequest = PageRequest.of(pageNumber, ConnectedUserRepository.DEFAULT_PAGE_SIZE);

        return connectedUserRepository.findAll(search, createDateFrom, createDateTo, integrationId, pageRequest);
    }
}

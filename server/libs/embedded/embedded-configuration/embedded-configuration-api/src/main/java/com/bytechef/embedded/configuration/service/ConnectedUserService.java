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
import java.time.LocalDate;
import org.springframework.data.domain.Page;

/**
 * @author Ivica Cardic
 */
public interface ConnectedUserService {

    void deleteConnectedUser(long id);

    void enableConnectedUser(long id, boolean enable);

    ConnectedUser getConnectedUser(long id);

    Page<ConnectedUser> getConnectedUsers(
        String name, LocalDate createDateFrom, LocalDate createDateTo, Long integrationId, int pageNumber);
}

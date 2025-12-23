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

package com.bytechef.ee.embedded.security.facade;

import com.bytechef.ee.embedded.security.domain.SigningKey;
import com.bytechef.ee.embedded.security.service.SigningKeyService;
import com.bytechef.platform.constant.PlatformType;
import com.bytechef.platform.user.domain.User;
import com.bytechef.platform.user.service.UserService;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author Ivica Cardic
 */
@Service
@Transactional
public class SigningKeyFacadeImpl implements SigningKeyFacade {

    private final SigningKeyService signingKeyService;
    private final UserService userService;

    @SuppressFBWarnings("EI")
    public SigningKeyFacadeImpl(SigningKeyService signingKeyService, UserService userService) {
        this.signingKeyService = signingKeyService;
        this.userService = userService;
    }

    @Override
    public String create(SigningKey signingKey, PlatformType type) {
        User user = userService.getCurrentUser();

        signingKey.setType(type);
        signingKey.setUserId(user.getId());

        return signingKeyService.create(signingKey);
    }
}

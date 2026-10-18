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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.platform.user.domain.PersistentToken;
import com.bytechef.platform.user.domain.User;
import com.bytechef.platform.user.event.UserDeletingEvent;
import com.bytechef.platform.user.repository.AuthorityRepository;
import com.bytechef.platform.user.repository.PersistentTokenRepository;
import com.bytechef.platform.user.repository.UserRepository;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * @author Ivica Cardic
 */
@ExtendWith(MockitoExtension.class)
class UserServiceDeleteTest {

    private static final String LOGIN = "user@localhost.com";

    @Mock
    private AuthorityRepository authorityRepository;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private PersistentTokenRepository persistentTokenRepository;

    @Mock
    private UserRepository userRepository;

    private UserService userService;

    @BeforeEach
    void setUp() {
        userService = new UserServiceImpl(
            authorityRepository, new ConcurrentMapCacheManager(), eventPublisher, passwordEncoder,
            persistentTokenRepository, userRepository);
    }

    @Test
    void testDeletePublishesUserDeletingEventBeforeDeletingTheUser() {
        User user = createUser();
        List<PersistentToken> persistentTokens = List.of(new PersistentToken());

        when(userRepository.findByLogin(LOGIN)).thenReturn(Optional.of(user));
        when(persistentTokenRepository.findAllByUserId(7L)).thenReturn(persistentTokens);

        userService.delete(LOGIN);

        InOrder inOrder = inOrder(eventPublisher, persistentTokenRepository, userRepository);

        inOrder.verify(eventPublisher)
            .publishEvent(new UserDeletingEvent(7L, LOGIN));
        inOrder.verify(persistentTokenRepository)
            .deleteAll(persistentTokens);
        inOrder.verify(userRepository)
            .delete(user);
    }

    @Test
    void testDeleteOfUnknownLoginPublishesNothing() {
        when(userRepository.findByLogin(anyString())).thenReturn(Optional.empty());

        userService.delete(LOGIN);

        verify(eventPublisher, never()).publishEvent(any(Object.class));
        verify(userRepository, never()).delete(any(User.class));
    }

    private static User createUser() {
        User user = new User();

        user.setId(7L);
        user.setLogin(LOGIN);
        user.setEmail(LOGIN);

        return user;
    }
}

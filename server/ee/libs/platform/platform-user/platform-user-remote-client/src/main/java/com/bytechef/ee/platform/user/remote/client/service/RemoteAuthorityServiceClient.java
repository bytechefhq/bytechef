/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.user.remote.client.service;

import com.bytechef.platform.user.domain.Authority;
import com.bytechef.platform.user.service.AuthorityService;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Component;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@Component
public class RemoteAuthorityServiceClient implements AuthorityService {

    @Override
    public Authority create(Authority authority) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void delete(long id) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Optional<Authority> fetchAuthority(long id) {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<Authority> getAuthorities() {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean exists(String name) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Authority update(Authority authority) {
        throw new UnsupportedOperationException();
    }
}

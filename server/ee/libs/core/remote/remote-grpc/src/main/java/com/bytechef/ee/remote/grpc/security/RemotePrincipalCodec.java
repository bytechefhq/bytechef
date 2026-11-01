/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.remote.grpc.security;

import io.grpc.Metadata;
import org.jspecify.annotations.Nullable;
import org.springframework.security.core.Authentication;

/**
 * Carries a principal kind owned by a module above the platform across gRPC calls. The client side writes the
 * principal's identifiers into the call metadata, and the receiving app rebuilds an equivalent {@link Authentication}
 * from them, so checks bound to that kind still run there.
 *
 * @version ee
 */
public interface RemotePrincipalCodec {

    String getPrincipalKind();

    boolean supports(Authentication authentication);

    boolean encode(Authentication authentication, Metadata metadata);

    @Nullable
    Authentication decode(String login, Metadata metadata);
}

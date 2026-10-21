/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.app.service;

import com.bytechef.ee.automation.app.domain.AppFavorite;
import java.util.Collection;
import java.util.List;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
public interface AppFavoriteService {

    void addFavorite(long userId, long appId);

    void deleteFavorites(long userId, Collection<Long> appIds);

    List<AppFavorite> getFavorites(long userId, Collection<Long> appIds);

    void removeFavorite(long userId, long appId);
}

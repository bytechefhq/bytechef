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

import com.bytechef.commons.util.OptionalUtils;
import com.bytechef.embedded.configuration.domain.AppEvent;
import com.bytechef.embedded.configuration.repository.AppEventRepository;
import java.util.List;
import org.apache.commons.lang3.Validate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author Ivica Cardic
 */
@Service
@Transactional
public class AppEventServiceImpl implements AppEventService {

    private final AppEventRepository appEventRepository;

    public AppEventServiceImpl(AppEventRepository appEventRepository) {
        this.appEventRepository = appEventRepository;
    }

    @Override
    public AppEvent create(AppEvent appEvent) {
        Validate.notNull(appEvent, "'appEvent' must not be null");
        Validate.isTrue(appEvent.getId() == null, "'id' must be null");
        Validate.notNull(appEvent.getName(), "'name' must not be null");
        Validate.notNull(appEvent.getSchema(), "'schema' must not be null");

        return appEventRepository.save(appEvent);
    }

    @Override
    public void delete(long id) {
        appEventRepository.deleteById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public AppEvent getAppEvent(long id) {
        return OptionalUtils.get(appEventRepository.findById(id));
    }

    @Override
    @Transactional(readOnly = true)
    public List<AppEvent> getAppEvents() {
        return appEventRepository.findAll();
    }

    @Override
    public AppEvent update(AppEvent appEvent) {
        AppEvent curAppEvent = getAppEvent(Validate.notNull(appEvent.getId(), "id"));

        curAppEvent.setName(Validate.notNull(appEvent.getName(), "name"));
        curAppEvent.setSchema(appEvent.getSchema());

        return appEventRepository.save(curAppEvent);
    }
}

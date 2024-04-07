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

package com.bytechef.platform.tag.service;

import com.bytechef.commons.util.OptionalUtils;
import com.bytechef.platform.tag.domain.Tag;
import com.bytechef.platform.tag.repository.TagRepository;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import org.apache.commons.lang3.Validate;
import org.springframework.data.domain.Sort;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author Ivica Cardic
 */
@Service
@Transactional
public class TagServiceImpl implements TagService {

    private final TagRepository tagRepository;

    @SuppressFBWarnings("EI2")
    public TagServiceImpl(TagRepository tagRepository) {
        this.tagRepository = tagRepository;
    }

    @Override
    public Tag create(Tag tag) {
        Validate.notNull(tag, "'tag' must not be null");
        Validate.isTrue(tag.getId() == null, "'tag.id' must be null");

        return tagRepository.save(tag);
    }

    @Override
    public void delete(long id) {
        tagRepository.deleteById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public Tag getTag(long id) {
        return OptionalUtils.get(tagRepository.findById(id));
    }

    @Override
    @Transactional(readOnly = true)
    public List<Tag> getTags() {
        return tagRepository.findAll(Sort.by("name"));
    }

    @Override
    @Transactional(readOnly = true)
    public List<Tag> getTags(@NonNull List<Long> ids) {
        Validate.notNull(ids, "'ids' must not be null");

        if (ids.isEmpty()) {
            return Collections.emptyList();
        } else {
            return tagRepository.findAllById(ids)
                .stream()
                .sorted(Comparator.comparing(Tag::getName))
                .toList();
        }
    }

    @Override
    public List<Tag> save(@NonNull List<Tag> tags) {
        Validate.notNull(tags, "'tags' must not be null");

        List<Tag> resultTags = new ArrayList<>();

        for (Tag tag : tags) {
            Validate.notNull(tag.getName(), "'name' must no be null");

            if (tag.isNew()) {
                OptionalUtils.ifPresentOrElse(
                    tagRepository.findByName(tag.getName()),
                    resultTags::add,
                    () -> resultTags.add(tagRepository.save(tag)));
            } else {
                Tag curTag = OptionalUtils.get(tagRepository.findById(Validate.notNull(tag.getId(), "id")));

                if (!Objects.equals(tag.getName(), curTag.getName())) {
                    curTag.setName(tag.getName());
                    curTag.setVersion(tag.getVersion());

                    curTag = tagRepository.save(curTag);
                }

                resultTags.add(curTag);
            }
        }

        return resultTags;
    }

    @Override
    public Tag update(Tag tag) {
        Validate.notNull(tag, "'tag' must not be null");
        Validate.notNull(tag.getId(), "'tag.id' must not be null");

        return tagRepository.save(tag);
    }
}

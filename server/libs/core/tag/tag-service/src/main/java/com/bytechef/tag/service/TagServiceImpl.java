
/*
 * Copyright 2021 <your company/name>.
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

package com.bytechef.tag.service;

import com.bytechef.tag.domain.Tag;
import com.bytechef.tag.repository.TagRepository;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.stream.StreamSupport;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.springframework.data.domain.Sort;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

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
        Assert.notNull(tag, "'tag' must not be null");
        Assert.isNull(tag.getId(), "'tag.id' must be null");

        return tagRepository.save(tag);
    }

    @Override
    public void delete(long id) {
        tagRepository.delete(getTag(id));
    }

    @Override
    public Tag getTag(long id) {
        return tagRepository.findById(id)
            .orElseThrow(IllegalArgumentException::new);
    }

    @Override
    public List<Tag> getTags() {
        return StreamSupport.stream(
            tagRepository.findAll(Sort.by("name"))
                .spliterator(),
            false)
            .toList();
    }

    @Override
    public List<Tag> getTags(@NonNull List<Long> ids) {
        Assert.notNull(ids, "'ids' must not be null");

        if (ids.isEmpty()) {
            return Collections.emptyList();
        } else {
            return StreamSupport.stream(
                tagRepository.findAllById(ids)
                    .spliterator(),
                false)
                .sorted(Comparator.comparing(Tag::getName))
                .toList();
        }
    }

    @Override
    @SuppressFBWarnings("NP")
    public List<Tag> save(@NonNull List<Tag> tags) {
        Assert.notNull(tags, "'tags' must not be null");

        List<Tag> resultTags = new ArrayList<>();

        for (Tag tag : tags) {
            if (tag.isNew()) {
                tagRepository.findByName(tag.getName())
                    .ifPresentOrElse(resultTags::add, () -> resultTags.add(tagRepository.save(tag)));
            } else {
                Tag curTag = tagRepository.findById(tag.getId())
                    .orElseThrow(IllegalArgumentException::new);

                curTag.setName(tag.getName());
                curTag.setVersion(tag.getVersion());

                resultTags.add(tagRepository.save(curTag));
            }
        }

        return resultTags;
    }

    @Override
    public Tag update(Tag tag) {
        Assert.notNull(tag, "'tag' must not be null");
        Assert.notNull(tag.getId(), "'tag.id' must not be null");

        return tagRepository.save(tag);
    }
}

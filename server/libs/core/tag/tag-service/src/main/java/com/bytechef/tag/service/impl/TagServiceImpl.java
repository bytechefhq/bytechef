
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

package com.bytechef.tag.service.impl;

import com.bytechef.tag.domain.Tag;
import com.bytechef.tag.repository.TagRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.StreamSupport;

import com.bytechef.tag.service.TagService;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
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
    public void delete(Long id) {
        tagRepository.deleteById(id);
    }

    @Override
    public List<Tag> getTags(@NonNull List<Long> ids) {
        return StreamSupport.stream(tagRepository.findAllById(ids)
            .spliterator(), false)
            .toList();
    }

    @Override
    @SuppressFBWarnings("NP")
    public List<Tag> save(@NonNull List<Tag> tags) {
        List<Tag> resultTags = new ArrayList<>();

        Assert.notNull(tags, "'tags' must not be null.");

        for (Tag tag : tags) {
            if (tag.isNew()) {
                tag.setId(null);

                tagRepository.findByName(tag.getName())
                    .ifPresentOrElse(resultTags::add, () -> resultTags.add(tagRepository.save(tag)));
            } else {
                Tag curTag = tagRepository.findById(tag.getId())
                    .orElseThrow();

                curTag.setName(tag.getName());
                curTag.setVersion(tag.getVersion());

                resultTags.add(tagRepository.save(curTag));
            }
        }

        return resultTags;
    }
}

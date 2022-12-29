
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

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.StreamSupport;

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
    public Set<Tag> create(@NonNull Set<String> tagNames) {
        Assert.notNull(tagNames, "'tags' must not be null.");

        Set<Tag> tags = new HashSet<>();

        for (String tagName : tagNames) {
            Optional<Tag> tagOptional = tagRepository.findByName(tagName);

            if (tagOptional.isPresent()) {
                tags.add(tagOptional.get());
            } else {
                tags.add(tagRepository.save(new Tag(tagName)));
            }
        }

        return tags;
    }

    @Override
    public boolean delete(Long id) {
        try {
            tagRepository.deleteById(id);
        } catch (Exception e) {
            // ignore

            return false;
        }

        return true;
    }

    @Override
    public List<Tag> getTags(@NonNull Set<Long> ids) {
        return StreamSupport.stream(tagRepository.findAllById(ids)
            .spliterator(), false)
            .toList();
    }
}

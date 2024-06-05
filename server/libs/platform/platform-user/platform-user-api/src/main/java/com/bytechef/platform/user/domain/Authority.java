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

package com.bytechef.platform.user.domain;

import java.util.Objects;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

/**
 * @author Ivica Cardic
 */
@Table
public class Authority {

    @Id
    private Long id;

    @Column
    private String name;

    public Long getId() {
        return this.id;
    }

    public String getName() {
        return this.name;
    }

    public Authority id(long id) {
        this.setId(id);

        return this;
    }

    public Authority name(String name) {
        this.setName(name);

        return this;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Authority authority)) {
            return false;
        }

        return Objects.equals(id, authority.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}

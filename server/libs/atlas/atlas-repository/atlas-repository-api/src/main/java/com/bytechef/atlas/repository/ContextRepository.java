
/*
 * Copyright 2016-2018 the original author or authors.
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
 *
 * Modifications copyright (C) 2021 <your company/name>
 */

package com.bytechef.atlas.repository;

import com.bytechef.atlas.domain.Context;
import org.springframework.data.repository.NoRepositoryBean;

/**
 * Stores context information for a job or task objects.
 *
 * <p>
 * {@link Context} instances are used to evaluate workflow tasks before they are executed.
 *
 * @author Arik Cohen
 * @since Mar 2017
 */
@NoRepositoryBean
public interface ContextRepository {

    Iterable<Context> findAll();

    Context findTop1ByStackIdAndClassnameIdOrderByCreatedDateDesc(long stackId, int classnameId);

    Context findTop1ByStackIdAndSubStackIdAndClassnameIdOrderByCreatedDateDesc(
        long stackId, int subStackId, int classnameId);

    Context save(Context context);
}

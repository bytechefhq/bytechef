/*
 * Copyright 2016-2018 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Modifications copyright (C) 2021 <your company/name>
 */

package com.integri.atlas.engine.core.error;

/**
 * An interface which marks an object as being
 * able to provide {@link Error} status about
 * itself.
 *
 * @author Arik Cohen
 * @since Apr 10, 2017
 */
public interface Errorable {
    /**
     * Returns the error associated with the object.
     */
    Error getError();
}

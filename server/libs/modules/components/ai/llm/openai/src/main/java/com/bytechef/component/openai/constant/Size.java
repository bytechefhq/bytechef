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

package com.bytechef.component.openai.constant;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

/**
 * @author Ivica Cardic
 */
public enum Size {

    DALL_E_2_256x256(new Integer[] {
        256, 256
    }),
    DALL_E_2_512x512(new Integer[] {
        512, 512
    }),
    _1024x1024(new Integer[] {
        1024, 1024
    }),
    DALL_E_3_1792x1024(new Integer[] {
        1792, 1024
    }),
    DALL_E_3_1024x1792(new Integer[] {
        1024, 1792
    });

    private final Integer[] dimensions;

    Size(Integer[] dimensions) {
        this.dimensions = dimensions;
    }

    @SuppressFBWarnings("EI")
    public Integer[] getDimensions() {
        return dimensions;
    }
}

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

package com.bytechef.component.data.stream.item;

import java.util.Map;
import org.springframework.batch.item.ItemStreamReader;
import org.springframework.batch.item.ItemStreamSupport;
import org.springframework.batch.item.NonTransientResourceException;
import org.springframework.batch.item.ParseException;
import org.springframework.batch.item.UnexpectedInputException;

/**
 * @author Ivica Cardic
 */
public class DataStreamItemReader extends ItemStreamSupport implements ItemStreamReader<Map<String, ?>> {

    @Override
    public Map<String, ?> read()
        throws Exception, UnexpectedInputException, ParseException, NonTransientResourceException {

        return null;
    }
}

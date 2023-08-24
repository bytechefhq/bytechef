
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

package com.bytechef.error;

import java.util.ArrayList;
import java.util.List;

import org.springframework.util.Assert;

/**
 * @author Arik Cohen
 * @author Ivica Cardic
 * @since Apr 10, 2017
 */
public class ErrorHandlerChain implements ErrorHandler<Errorable> {

    private final List<? extends ErrorHandler<? super Errorable>> errorHandlers;

    public ErrorHandlerChain(List<? extends ErrorHandler<? super Errorable>> errorHandlers) {
        Assert.notNull(errorHandlers, "'errorHandlers' must not be null");

        this.errorHandlers = new ArrayList<>(errorHandlers);
    }

    @Override
    public void handle(Errorable errorable) {
        for (ErrorHandler<? super Errorable> errorHandler : errorHandlers) {
            Class<?> type = errorHandler.getType();

            if (type.isAssignableFrom(errorable.getClass())) {
                errorHandler.handle(errorable);
            }
        }
    }

    @Override
    public Class<?> getType() {
        return null;
    }
}

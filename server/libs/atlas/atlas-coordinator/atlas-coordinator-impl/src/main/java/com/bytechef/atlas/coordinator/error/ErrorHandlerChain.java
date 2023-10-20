
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

package com.bytechef.atlas.coordinator.error;

import com.bytechef.atlas.error.ErrorHandler;
import com.bytechef.atlas.error.Errorable;
import java.lang.reflect.Method;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.util.Assert;

/**
 * @author Arik Cohen
 * @since Apr 10, 2017
 */
public class ErrorHandlerChain implements ErrorHandler<Errorable> {

    private static final Logger logger = LoggerFactory.getLogger(ErrorHandlerChain.class);

    private final List<? extends ErrorHandler<? super Errorable>> errorHandlers;

    public ErrorHandlerChain(List<? extends ErrorHandler<? super Errorable>> errorHandlers) {
        Assert.notNull(errorHandlers, "'errorHandlers' must not be null.");

        this.errorHandlers = errorHandlers;
    }

    @Override
    public void handle(Errorable errorable) {
        for (ErrorHandler<? super Errorable> handler : errorHandlers) {
            Method method = BeanUtils.findDeclaredMethodWithMinimalParameters(handler.getClass(), "handle");

            if (method == null) {
                logger.error("Unable to locate handle method for " + handler.getClass());

                return;
            }

            Class<?> type = method.getParameters()[0].getType();

            if (type.isAssignableFrom(errorable.getClass())) {
                handler.handle(errorable);
            }
        }
    }
}

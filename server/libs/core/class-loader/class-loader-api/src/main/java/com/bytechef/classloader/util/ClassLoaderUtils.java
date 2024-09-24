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

package com.bytechef.classloader.util;

/**
 * @author Ivica Cardic
 */
public class ClassLoaderUtils {

    public static <A> A loadWithClassLoader(ClassLoader classLoader, java.util.concurrent.Callable<A> task) {
        Thread currentThread = Thread.currentThread();

        ClassLoader previousClassloader = currentThread.getContextClassLoader();

        currentThread.setContextClassLoader(classLoader);

        try {
            return task.call();
        } catch (Exception e) {
            throw new RuntimeException(e);
        } finally {
            currentThread.setContextClassLoader(previousClassloader);
        }
    }
}

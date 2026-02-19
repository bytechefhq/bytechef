/*
 * Copyright 2025 ByteChef
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

package com.bytechef.cli.command.component.init;

import com.bytechef.cli.CliApplication;
import java.io.File;
import java.net.URL;
import org.junit.jupiter.api.Test;

/**
 * @author Ivica Cardic
 */
public class ComponentInitCommandTest {

    @Test
    public void testComponentInit() {
        URL url = ComponentInitCommandTest.class.getResource("/dependencies/petstore.yaml");

        CliApplication.main(
            "component", "init", "--open-api-path", url.getFile(), "--output-path",
            new File("build/test/generated").getAbsolutePath(), "-n", "petstore");

        // TODO Add asserts
    }

    @Test
    public void testComponentInit2() {
        URL url = ComponentInitCommandTest.class.getResource("/dependencies/petstore2.yaml");

        CliApplication.main(
            "component", "init", "--open-api-path", url.getFile(), "--output-path",
            new File("build/test/generated").getAbsolutePath(), "-n", "petstore");

        // TODO Add asserts
    }
}

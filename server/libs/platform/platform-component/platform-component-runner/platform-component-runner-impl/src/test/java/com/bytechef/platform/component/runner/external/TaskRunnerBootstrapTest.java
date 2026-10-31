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

package com.bytechef.platform.component.runner.external;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

/**
 * @author Ivica Cardic
 */
class TaskRunnerBootstrapTest {

    @Test
    void testUserSourceKeepsItsLineNumbers() {
        String source = "function perform(input, context) {\n    return input;\n}\n";

        String appended = TaskRunnerBootstrap.append("javascript", source, "process");

        assertThat(appended).startsWith(source);
    }

    @Test
    void testJavaScriptBootstrapReadsInputAndWritesOutput() {
        String appended = TaskRunnerBootstrap.append("javascript", "function perform() {}\n", "process");

        assertThat(appended).contains("BYTECHEF_INPUT_FILE");
        assertThat(appended).contains("BYTECHEF_OUTPUT_DIR");
        assertThat(appended).contains("output.json");
        assertThat(appended).contains("perform(");
    }

    @Test
    void testPythonBootstrapReadsInputAndWritesOutput() {
        String appended = TaskRunnerBootstrap.append(
            "python", "def perform(input, context):\n    return 1\n", "process");

        assertThat(appended).contains("BYTECHEF_INPUT_FILE");
        assertThat(appended).contains("BYTECHEF_OUTPUT_DIR");
        assertThat(appended).contains("output.json");
        assertThat(appended).contains("perform(");
    }

    @Test
    void testContextStubNamesTheRunnerInItsError() {
        assertThat(TaskRunnerBootstrap.append("javascript", "function perform() {}\n", "docker"))
            .contains("is not available under the docker runner");
        assertThat(TaskRunnerBootstrap.append("python", "def perform(a, b):\n    pass\n", "process"))
            .contains("is not available under the process runner");
    }

    @Test
    void testSourceIsSeparatedFromTheBootstrapByANewline() {
        String appended = TaskRunnerBootstrap.append("javascript", "function perform() {}", "process");

        assertThat(appended).startsWith("function perform() {}\n");
    }

    @Test
    void testTheTruffleJavaScriptIdIsTheSameLanguageAsJavascript() {
        assertThat(TaskRunnerBootstrap.isSupported("js")).isTrue();
        assertThat(TaskRunnerBootstrap.sourceFileName("js")).isEqualTo("script.js");
        assertThat(TaskRunnerBootstrap.append("js", "function perform() {}\n", "process"))
            .isEqualTo(TaskRunnerBootstrap.append("javascript", "function perform() {}\n", "process"));
    }

    @Test
    void testSourceFileNamePerLanguage() {
        assertThat(TaskRunnerBootstrap.sourceFileName("javascript")).isEqualTo("script.js");
        assertThat(TaskRunnerBootstrap.sourceFileName("python")).isEqualTo("script.py");
        assertThat(TaskRunnerBootstrap.sourceFileName("shell")).isEqualTo("commands.sh");
        assertThat(TaskRunnerBootstrap.sourceFileName("bash")).isEqualTo("script.sh");
    }

    @Test
    void testTheInProcessOnlyLanguagesAreNotSupported() {
        assertThat(TaskRunnerBootstrap.isSupported("ruby")).isFalse();
        assertThat(TaskRunnerBootstrap.isSupported("java")).isFalse();
        assertThat(TaskRunnerBootstrap.isSupported("R")).isFalse();
    }

    @Test
    void testUnsupportedLanguageIsRejected() {
        assertThatThrownBy(() -> TaskRunnerBootstrap.append("cobol", "x", "process"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("cobol");
    }

    @Test
    void testShellNeedsNoBootstrap() {
        assertThat(TaskRunnerBootstrap.append("shell", "echo hi\n", "process")).isEqualTo("echo hi\n");
        assertThat(TaskRunnerBootstrap.append("bash", "echo hi", "process")).isEqualTo("echo hi\n");
    }
}

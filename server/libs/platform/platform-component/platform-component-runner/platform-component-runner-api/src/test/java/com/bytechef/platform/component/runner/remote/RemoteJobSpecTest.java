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

package com.bytechef.platform.component.runner.remote;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.bytechef.platform.component.definition.ParametersFactory;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

public class RemoteJobSpecTest {

    @Test
    public void testRemotePathsHangOffTheBackendsWorkingDirectory() {
        RemoteJobSpec spec = spec("/bytechef/work");

        assertThat(spec.remoteSourcePath()).isEqualTo("/bytechef/work/script.py");
        assertThat(spec.remoteInputFilePath()).isEqualTo("/bytechef/work/input.json");
        assertThat(spec.remoteOutputDirectory()).isEqualTo("/bytechef/work/output");
        assertThat(spec.remoteStderrPath()).isEqualTo("/bytechef/work/.stderr");
    }

    @Test
    public void testATrailingSlashOnTheWorkingDirectoryIsTolerated() {
        assertThat(spec("/tmp/").remoteSourcePath()).isEqualTo("/tmp/script.py");
    }

    @Test
    public void testTheWorkingDirectoryMustBeAbsolute() {
        assertThatThrownBy(() -> spec("work"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("absolute");
    }

    @Test
    public void testTheKeyBecomesTheLabelValue() {
        assertThat(new RemoteJobKey("1234", 9L).value()).isEqualTo("1234");
        assertThat(new RemoteJobKey("1234", null).jobId()).isNull();
        assertThatThrownBy(() -> new RemoteJobKey(" ", null)).isInstanceOf(IllegalArgumentException.class);
    }

    private static RemoteJobSpec spec(String remoteWorkingDirectory) {
        return new RemoteJobSpec(
            new RemoteJobKey("1", null), "python", "script.py", List.of("python3"), Map.of(), remoteWorkingDirectory,
            RemotePullPolicy.IF_NOT_PRESENT, ParametersFactory.create(Map.of()));
    }
}

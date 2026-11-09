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

package com.bytechef.platform.ai.workspace;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.junit.jupiter.api.io.TempDir;

/**
 * @author Ivica Cardic
 */
class AgentWorkspaceQuotaTest {

    @TempDir
    private Path outsideDirectory;

    @TempDir
    private Path root;

    @Test
    void testGetUsedBytesSumsEveryFileInTheWorkspace() throws IOException {
        Files.writeString(root.resolve("a.txt"), "12345");
        Files.writeString(Files.createDirectories(root.resolve("nested/deeper"))
            .resolve("b.txt"), "1234567890");

        assertThat(new AgentWorkspaceQuota(root, 100).getUsedBytes()).isEqualTo(15);
    }

    @Test
    @DisabledOnOs(OS.WINDOWS)
    void testGetUsedBytesDoesNotCountFilesBehindSymbolicLinks() throws IOException {
        Files.writeString(outsideDirectory.resolve("large.txt"), "x".repeat(1000));

        Files.createSymbolicLink(root.resolve("outside"), outsideDirectory);

        assertThat(new AgentWorkspaceQuota(root, 100).getUsedBytes()).isZero();
    }

    @Test
    void testGetUsedBytesOfAMissingWorkspaceIsZero() {
        assertThat(new AgentWorkspaceQuota(root.resolve("missing"), 100).getUsedBytes()).isZero();
    }

    @Test
    void testIsExceededComparesTheUsedBytesWithTheLimit() throws IOException {
        Files.writeString(root.resolve("a.txt"), "1234567890");

        assertThat(new AgentWorkspaceQuota(root, 10).isExceeded()).isFalse();
        assertThat(new AgentWorkspaceQuota(root, 9).isExceeded()).isTrue();
    }

    @Test
    void testAZeroLimitMeansNoLimit() throws IOException {
        Files.writeString(root.resolve("a.txt"), "1234567890");

        AgentWorkspaceQuota quota = new AgentWorkspaceQuota(root, 0);

        assertThat(quota.isLimited()).isFalse();
        assertThat(quota.isExceeded()).isFalse();
    }

    @Test
    void testFormatSizeUsesTheLargestFittingUnit() {
        assertThat(AgentWorkspaceQuota.formatSize(512)).isEqualTo("512 bytes");
        assertThat(AgentWorkspaceQuota.formatSize(1536)).isEqualTo("1.5 KB");
        assertThat(AgentWorkspaceQuota.formatSize(500L * 1024 * 1024)).isEqualTo("500.0 MB");
        assertThat(AgentWorkspaceQuota.formatSize(1024L * 1024 * 1024)).isEqualTo("1.0 GB");
    }

    @Test
    void testGetFullMessageNamesTheUsageTheLimitAndTheWrite() {
        AgentWorkspaceQuota quota = new AgentWorkspaceQuota(root, 500L * 1024 * 1024);

        assertThat(quota.getFullMessage(498L * 1024 * 1024, 12L * 1024 * 1024)).isEqualTo(
            "Error: the agent workspace is full: 498.0 MB of 500.0 MB used, this write needs 12.0 MB. Delete files " +
                "you no longer need.");
    }

    @Test
    void testIsCleanupCommandAcceptsOnlyDeletingCommands() {
        assertThat(AgentWorkspaceQuota.isCleanupCommand("rm -rf build")).isTrue();
        assertThat(AgentWorkspaceQuota.isCleanupCommand("  truncate -s 0 log.txt")).isTrue();
        assertThat(AgentWorkspaceQuota.isCleanupCommand("find . -name '*.tmp' -delete")).isTrue();
        assertThat(AgentWorkspaceQuota.isCleanupCommand("git clone https://example.com/repo.git")).isFalse();
        assertThat(AgentWorkspaceQuota.isCleanupCommand(null)).isFalse();
    }

    @Test
    void testIsCleanupCommandRefusesACleanupCommandChainedWithAnotherCommand() {
        assertThat(AgentWorkspaceQuota.isCleanupCommand("rm a; dd if=/dev/zero of=big bs=1M count=100")).isFalse();
        assertThat(AgentWorkspaceQuota.isCleanupCommand("rm a && curl -o big https://example.com")).isFalse();
        assertThat(AgentWorkspaceQuota.isCleanupCommand("rm a || yes > big")).isFalse();
        assertThat(AgentWorkspaceQuota.isCleanupCommand("rm a | tee big")).isFalse();
        assertThat(AgentWorkspaceQuota.isCleanupCommand("rm $(cp -r src copy)")).isFalse();
        assertThat(AgentWorkspaceQuota.isCleanupCommand("rm `cp -r src copy`")).isFalse();
        assertThat(AgentWorkspaceQuota.isCleanupCommand("rm a\ncp -r src copy")).isFalse();
    }

    @Test
    void testIsCleanupCommandRefusesCommandsThatCanGrowTheWorkspace() {
        assertThat(AgentWorkspaceQuota.isCleanupCommand("truncate -s 10G big")).isFalse();
        assertThat(AgentWorkspaceQuota.isCleanupCommand("truncate --size=0 log.txt")).isTrue();
        assertThat(AgentWorkspaceQuota.isCleanupCommand("find . -exec cp {} {}.bak \\; -delete")).isFalse();
        assertThat(AgentWorkspaceQuota.isCleanupCommand("find . -fprint list.txt -delete")).isFalse();
    }

    @Test
    void testGetDirectorySizeSumsEveryFileInTheDirectory() throws IOException {
        Path directory = Files.createDirectories(root.resolve("skills/one"));

        Files.write(directory.resolve("SKILL.md"), new byte[25]);

        assertThat(AgentWorkspaceQuota.getDirectorySize(root.resolve("skills"))).isEqualTo(25);
    }

    @Test
    void testGetWriteRequiredBytesSubtractsTheReplacedFile() throws IOException {
        AgentWorkspaceQuota quota = new AgentWorkspaceQuota(root, 100);

        Path file = Files.writeString(root.resolve("file.txt"), "x".repeat(40));

        assertThat(quota.getWriteRequiredBytes(file, "y".repeat(50))).isEqualTo(10);
        assertThat(quota.getWriteRequiredBytes(root.resolve("new.txt"), "y".repeat(50))).isEqualTo(50);
        assertThat(quota.getWriteRequiredBytes(file, "y")).isNegative();
    }

    @Test
    void testGetEditRequiredBytesMultipliesTheGrowthForReplaceAll() throws IOException {
        AgentWorkspaceQuota quota = new AgentWorkspaceQuota(root, 100);

        Path file = Files.writeString(root.resolve("file.txt"), "a-a-a");

        assertThat(quota.getEditRequiredBytes(file, "a", "abc", false)).isEqualTo(2);
        assertThat(quota.getEditRequiredBytes(file, "a", "abc", true)).isEqualTo(6);
        assertThat(quota.getEditRequiredBytes(file, "abc", "a", true)).isZero();
    }

    @Test
    void testCheckWriteRefusesOnlyAWriteThatGoesOverTheLimit() throws IOException {
        AgentWorkspaceQuota quota = new AgentWorkspaceQuota(root, 100);

        Files.writeString(root.resolve("file.txt"), "x".repeat(80));

        assertThat(quota.checkWrite(20)).isNull();
        assertThat(quota.checkWrite(21)).contains("workspace is full");
        assertThat(quota.checkWrite(-50)).isNull();
        assertThat(new AgentWorkspaceQuota(root, 0).checkWrite(1000)).isNull();
    }
}

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

package com.bytechef.ai.copilot.service;

/**
 * Synchronously drives the {@code skills_build} agent end-to-end so a caller can create a fresh skill from a single
 * prompt: the empty skill is created first and its id passed in, then the agent autonomously fills its SKILL.md.
 *
 * @author Ivica Cardic
 */
public interface CopilotSkillGenerator {

    void generateSkill(long skillId, String prompt);
}

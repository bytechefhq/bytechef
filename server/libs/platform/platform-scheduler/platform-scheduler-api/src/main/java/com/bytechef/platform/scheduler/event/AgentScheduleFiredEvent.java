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

package com.bytechef.platform.scheduler.event;

/**
 * Application event published by {@code AgentScheduleJob} when a scheduled agent run fires. Consumers in EE modules
 * (e.g., AI Hub) subscribe via {@code @EventListener} and do the actual task creation / LLM dispatch. Keeps
 * platform-scheduler free of EE dependencies.
 *
 * @author Ivica Cardic
 */
public record AgentScheduleFiredEvent(long agentScheduleId) {
}

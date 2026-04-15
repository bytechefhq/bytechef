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

package com.bytechef.component.ai.agenticai.embabel

/**
 * Signals that an Embabel GOAP plan terminated without producing a usable value at the configured
 * goal output binding. This distinguishes three previously-silent failure modes:
 *   - no value written to the binding (planner gave up, budget exhausted, or goal unreachable),
 *   - a value of an unexpected type was written (contract break with Embabel's storage model),
 *   - a [Binding] was produced but its content was empty.
 *
 * Callers receive explicit diagnostic context instead of a success-looking placeholder string.
 */
class AgenticAiGoalNotAchievedException(message: String) : RuntimeException(message)

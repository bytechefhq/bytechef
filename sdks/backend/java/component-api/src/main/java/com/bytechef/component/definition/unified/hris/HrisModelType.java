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

package com.bytechef.component.definition.unified.hris;

import com.bytechef.component.definition.UnifiedApiDefinition;

/**
 * HRIS model type.
 *
 * @author Ivica Cardic
 */
public enum HrisModelType implements UnifiedApiDefinition.ModelType {

    BANK_INFO,
    BENEFIT,
    COMPANY,
    DEPENDENT,
    EMPLOYEE_PAYROLL_RUN,
    EMPLOYEE,
    EMPLOYER_BENEFIT,
    EMPLOYMENT,
    GROUP,
    LOCATION,
    PAY_GROUP,
    PAYROLL_RUN,
    TIME_OFF,
    TIME_OFF_BALANCE,
    TIMESHEET_ENTRY
}

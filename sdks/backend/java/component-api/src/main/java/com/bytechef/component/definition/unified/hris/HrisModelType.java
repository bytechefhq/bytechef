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

package com.bytechef.component.definition.unified.hris;

import com.bytechef.component.definition.UnifiedApiDefinition;

/**
 * Enumerates the unified model types exposed by the HRIS (Human Resources Information System) category of the unified
 * API, covering the workforce, payroll, and benefits entities that HR platforms have in common.
 *
 * @author Ivica Cardic
 */
public enum HrisModelType implements UnifiedApiDefinition.ModelType {

    /** An employee's bank account details used for payroll deposits. */
    BANK_INFO,
    /** A benefit plan an employee is enrolled in. */
    BENEFIT,
    /** The employing company or organization. */
    COMPANY,
    /** A dependent of an employee, such as for benefits coverage. */
    DEPENDENT,
    /** An employee's participation in a specific payroll run. */
    EMPLOYEE_PAYROLL_RUN,
    /** A person employed by the company. */
    EMPLOYEE,
    /** A benefit plan offered by the employer. */
    EMPLOYER_BENEFIT,
    /** An employment record describing an employee's role and terms. */
    EMPLOYMENT,
    /** A group used to organize employees. */
    GROUP,
    /** A physical work location. */
    LOCATION,
    /** A pay group that determines an employee's pay schedule. */
    PAY_GROUP,
    /** A payroll run covering a pay period. */
    PAYROLL_RUN,
    /** A time-off request or record. */
    TIME_OFF,
    /** An employee's remaining time-off balance. */
    TIME_OFF_BALANCE,
    /** A single timesheet entry recording hours worked. */
    TIMESHEET_ENTRY
}

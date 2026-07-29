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

package com.bytechef.component.definition.unified.ats;

import com.bytechef.component.definition.UnifiedApiDefinition;

/**
 * Enumerates the unified model types exposed by the ATS (Applicant Tracking System) category of the unified API,
 * covering the recruiting entities that hiring platforms have in common.
 *
 * @author Ivica Cardic
 */
public enum AtsModelType implements UnifiedApiDefinition.ModelType {

    /** An activity or event logged against a candidate or application. */
    ACTIVITY,
    /** A candidate's application to a specific job. */
    APPLICATION,
    /** A file attached to another ATS record, such as a resume. */
    ATTACHMENT,
    /** A person being considered for one or more jobs. */
    CANDIDATE,
    /** An organizational department that jobs belong to. */
    DEPARTMENT,
    /** Equal Employment Opportunity Commission demographic data for a candidate. */
    EEOC,
    /** A scheduled interview in the hiring process. */
    INTERVIEW,
    /** A stage within a job's interview pipeline. */
    JOB_INTERVIEW_STAGE,
    /** An open job or requisition. */
    JOB,
    /** An offer extended to a candidate. */
    OFFER,
    /** A physical office or work location. */
    OFFICE,
    /** A reason a candidate or application was rejected. */
    REJECT_REASON,
    /** An interviewer's evaluation scorecard for a candidate. */
    SCORECARD,
    /** A label used to categorize candidates or applications. */
    TAG,
    /** A recruiter or other user of the ATS. */
    USER
}

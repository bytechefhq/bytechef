/* tslint:disable */
/* eslint-disable */
/**
 * The Automation Execution Internal API
 * No description provided (generated by Openapi Generator https://github.com/openapitools/openapi-generator)
 *
 * The version of the OpenAPI document: 1
 * 
 *
 * NOTE: This class is auto generated by OpenAPI Generator (https://openapi-generator.tech).
 * https://openapi-generator.tech
 * Do not edit the class manually.
 */

import { mapValues } from '../runtime';
import type { WorkflowBasic } from './WorkflowBasic';
import {
    WorkflowBasicFromJSON,
    WorkflowBasicFromJSONTyped,
    WorkflowBasicToJSON,
    WorkflowBasicToJSONTyped,
} from './WorkflowBasic';
import type { TriggerExecution } from './TriggerExecution';
import {
    TriggerExecutionFromJSON,
    TriggerExecutionFromJSONTyped,
    TriggerExecutionToJSON,
    TriggerExecutionToJSONTyped,
} from './TriggerExecution';
import type { ProjectInstanceBasic } from './ProjectInstanceBasic';
import {
    ProjectInstanceBasicFromJSON,
    ProjectInstanceBasicFromJSONTyped,
    ProjectInstanceBasicToJSON,
    ProjectInstanceBasicToJSONTyped,
} from './ProjectInstanceBasic';
import type { Job } from './Job';
import {
    JobFromJSON,
    JobFromJSONTyped,
    JobToJSON,
    JobToJSONTyped,
} from './Job';
import type { ProjectBasic } from './ProjectBasic';
import {
    ProjectBasicFromJSON,
    ProjectBasicFromJSONTyped,
    ProjectBasicToJSON,
    ProjectBasicToJSONTyped,
} from './ProjectBasic';

/**
 * Contains information about execution of a project workflow.
 * @export
 * @interface WorkflowExecution
 */
export interface WorkflowExecution {
    /**
     * The id of a workflow execution.
     * @type {number}
     * @memberof WorkflowExecution
     */
    readonly id?: number;
    /**
     * 
     * @type {Job}
     * @memberof WorkflowExecution
     */
    job?: Job;
    /**
     * 
     * @type {ProjectBasic}
     * @memberof WorkflowExecution
     */
    project?: ProjectBasic;
    /**
     * 
     * @type {ProjectInstanceBasic}
     * @memberof WorkflowExecution
     */
    projectInstance?: ProjectInstanceBasic;
    /**
     * 
     * @type {TriggerExecution}
     * @memberof WorkflowExecution
     */
    triggerExecution?: TriggerExecution;
    /**
     * 
     * @type {WorkflowBasic}
     * @memberof WorkflowExecution
     */
    workflow?: WorkflowBasic;
}

/**
 * Check if a given object implements the WorkflowExecution interface.
 */
export function instanceOfWorkflowExecution(value: object): value is WorkflowExecution {
    return true;
}

export function WorkflowExecutionFromJSON(json: any): WorkflowExecution {
    return WorkflowExecutionFromJSONTyped(json, false);
}

export function WorkflowExecutionFromJSONTyped(json: any, ignoreDiscriminator: boolean): WorkflowExecution {
    if (json == null) {
        return json;
    }
    return {
        
        'id': json['id'] == null ? undefined : json['id'],
        'job': json['job'] == null ? undefined : JobFromJSON(json['job']),
        'project': json['project'] == null ? undefined : ProjectBasicFromJSON(json['project']),
        'projectInstance': json['projectInstance'] == null ? undefined : ProjectInstanceBasicFromJSON(json['projectInstance']),
        'triggerExecution': json['triggerExecution'] == null ? undefined : TriggerExecutionFromJSON(json['triggerExecution']),
        'workflow': json['workflow'] == null ? undefined : WorkflowBasicFromJSON(json['workflow']),
    };
}

export function WorkflowExecutionToJSON(json: any): WorkflowExecution {
    return WorkflowExecutionToJSONTyped(json, false);
}

export function WorkflowExecutionToJSONTyped(value?: Omit<WorkflowExecution, 'id'> | null, ignoreDiscriminator: boolean = false): any {
    if (value == null) {
        return value;
    }

    return {
        
        'job': JobToJSON(value['job']),
        'project': ProjectBasicToJSON(value['project']),
        'projectInstance': ProjectInstanceBasicToJSON(value['projectInstance']),
        'triggerExecution': TriggerExecutionToJSON(value['triggerExecution']),
        'workflow': WorkflowBasicToJSON(value['workflow']),
    };
}


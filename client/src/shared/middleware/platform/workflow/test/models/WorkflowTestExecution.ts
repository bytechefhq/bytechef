/* tslint:disable */
/* eslint-disable */
/**
 * The Platform Workflow Test Internal API
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
import type { TriggerExecution } from './TriggerExecution';
import {
    TriggerExecutionFromJSON,
    TriggerExecutionFromJSONTyped,
    TriggerExecutionToJSON,
    TriggerExecutionToJSONTyped,
} from './TriggerExecution';
import type { Job } from './Job';
import {
    JobFromJSON,
    JobFromJSONTyped,
    JobToJSON,
    JobToJSONTyped,
} from './Job';

/**
 * Contains information about test execution of a workflow.
 * @export
 * @interface WorkflowTestExecution
 */
export interface WorkflowTestExecution {
    /**
     * 
     * @type {Job}
     * @memberof WorkflowTestExecution
     */
    job?: Job;
    /**
     * 
     * @type {TriggerExecution}
     * @memberof WorkflowTestExecution
     */
    triggerExecution?: TriggerExecution;
}

/**
 * Check if a given object implements the WorkflowTestExecution interface.
 */
export function instanceOfWorkflowTestExecution(value: object): value is WorkflowTestExecution {
    return true;
}

export function WorkflowTestExecutionFromJSON(json: any): WorkflowTestExecution {
    return WorkflowTestExecutionFromJSONTyped(json, false);
}

export function WorkflowTestExecutionFromJSONTyped(json: any, ignoreDiscriminator: boolean): WorkflowTestExecution {
    if (json == null) {
        return json;
    }
    return {
        
        'job': json['job'] == null ? undefined : JobFromJSON(json['job']),
        'triggerExecution': json['triggerExecution'] == null ? undefined : TriggerExecutionFromJSON(json['triggerExecution']),
    };
}

export function WorkflowTestExecutionToJSON(json: any): WorkflowTestExecution {
    return WorkflowTestExecutionToJSONTyped(json, false);
}

export function WorkflowTestExecutionToJSONTyped(value?: WorkflowTestExecution | null, ignoreDiscriminator: boolean = false): any {
    if (value == null) {
        return value;
    }

    return {
        
        'job': JobToJSON(value['job']),
        'triggerExecution': TriggerExecutionToJSON(value['triggerExecution']),
    };
}


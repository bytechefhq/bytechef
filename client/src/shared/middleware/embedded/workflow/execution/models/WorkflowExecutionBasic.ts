/* tslint:disable */
/* eslint-disable */
/**
 * Embedded Execution Internal API
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
import type { JobBasic } from './JobBasic';
import {
    JobBasicFromJSON,
    JobBasicFromJSONTyped,
    JobBasicToJSON,
    JobBasicToJSONTyped,
} from './JobBasic';
import type { IntegrationBasic } from './IntegrationBasic';
import {
    IntegrationBasicFromJSON,
    IntegrationBasicFromJSONTyped,
    IntegrationBasicToJSON,
    IntegrationBasicToJSONTyped,
} from './IntegrationBasic';
import type { IntegrationInstanceConfigurationBasic } from './IntegrationInstanceConfigurationBasic';
import {
    IntegrationInstanceConfigurationBasicFromJSON,
    IntegrationInstanceConfigurationBasicFromJSONTyped,
    IntegrationInstanceConfigurationBasicToJSON,
    IntegrationInstanceConfigurationBasicToJSONTyped,
} from './IntegrationInstanceConfigurationBasic';
import type { IntegrationInstanceBasic } from './IntegrationInstanceBasic';
import {
    IntegrationInstanceBasicFromJSON,
    IntegrationInstanceBasicFromJSONTyped,
    IntegrationInstanceBasicToJSON,
    IntegrationInstanceBasicToJSONTyped,
} from './IntegrationInstanceBasic';

/**
 * Contains information about execution of a Integration workflow.
 * @export
 * @interface WorkflowExecutionBasic
 */
export interface WorkflowExecutionBasic {
    /**
     * The id of a workflow execution.
     * @type {number}
     * @memberof WorkflowExecutionBasic
     */
    readonly id?: number;
    /**
     * 
     * @type {IntegrationBasic}
     * @memberof WorkflowExecutionBasic
     */
    integration?: IntegrationBasic;
    /**
     * 
     * @type {IntegrationInstanceConfigurationBasic}
     * @memberof WorkflowExecutionBasic
     */
    integrationInstanceConfiguration?: IntegrationInstanceConfigurationBasic;
    /**
     * 
     * @type {IntegrationInstanceBasic}
     * @memberof WorkflowExecutionBasic
     */
    integrationInstance?: IntegrationInstanceBasic;
    /**
     * 
     * @type {JobBasic}
     * @memberof WorkflowExecutionBasic
     */
    job?: JobBasic;
    /**
     * 
     * @type {WorkflowBasic}
     * @memberof WorkflowExecutionBasic
     */
    workflow?: WorkflowBasic;
}

/**
 * Check if a given object implements the WorkflowExecutionBasic interface.
 */
export function instanceOfWorkflowExecutionBasic(value: object): value is WorkflowExecutionBasic {
    return true;
}

export function WorkflowExecutionBasicFromJSON(json: any): WorkflowExecutionBasic {
    return WorkflowExecutionBasicFromJSONTyped(json, false);
}

export function WorkflowExecutionBasicFromJSONTyped(json: any, ignoreDiscriminator: boolean): WorkflowExecutionBasic {
    if (json == null) {
        return json;
    }
    return {
        
        'id': json['id'] == null ? undefined : json['id'],
        'integration': json['integration'] == null ? undefined : IntegrationBasicFromJSON(json['integration']),
        'integrationInstanceConfiguration': json['integrationInstanceConfiguration'] == null ? undefined : IntegrationInstanceConfigurationBasicFromJSON(json['integrationInstanceConfiguration']),
        'integrationInstance': json['integrationInstance'] == null ? undefined : IntegrationInstanceBasicFromJSON(json['integrationInstance']),
        'job': json['job'] == null ? undefined : JobBasicFromJSON(json['job']),
        'workflow': json['workflow'] == null ? undefined : WorkflowBasicFromJSON(json['workflow']),
    };
}

export function WorkflowExecutionBasicToJSON(json: any): WorkflowExecutionBasic {
    return WorkflowExecutionBasicToJSONTyped(json, false);
}

export function WorkflowExecutionBasicToJSONTyped(value?: Omit<WorkflowExecutionBasic, 'id'> | null, ignoreDiscriminator: boolean = false): any {
    if (value == null) {
        return value;
    }

    return {
        
        'integration': IntegrationBasicToJSON(value['integration']),
        'integrationInstanceConfiguration': IntegrationInstanceConfigurationBasicToJSON(value['integrationInstanceConfiguration']),
        'integrationInstance': IntegrationInstanceBasicToJSON(value['integrationInstance']),
        'job': JobBasicToJSON(value['job']),
        'workflow': WorkflowBasicToJSON(value['workflow']),
    };
}


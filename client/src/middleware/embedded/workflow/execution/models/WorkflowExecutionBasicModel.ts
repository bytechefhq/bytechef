/* tslint:disable */
/* eslint-disable */
/**
 * Embedded Execution API
 * No description provided (generated by Openapi Generator https://github.com/openapitools/openapi-generator)
 *
 * The version of the OpenAPI document: 1
 * 
 *
 * NOTE: This class is auto generated by OpenAPI Generator (https://openapi-generator.tech).
 * https://openapi-generator.tech
 * Do not edit the class manually.
 */

import { exists, mapValues } from '../runtime';
import type { IntegrationBasicModel } from './IntegrationBasicModel';
import {
    IntegrationBasicModelFromJSON,
    IntegrationBasicModelFromJSONTyped,
    IntegrationBasicModelToJSON,
} from './IntegrationBasicModel';
import type { IntegrationInstanceBasicModel } from './IntegrationInstanceBasicModel';
import {
    IntegrationInstanceBasicModelFromJSON,
    IntegrationInstanceBasicModelFromJSONTyped,
    IntegrationInstanceBasicModelToJSON,
} from './IntegrationInstanceBasicModel';
import type { JobBasicModel } from './JobBasicModel';
import {
    JobBasicModelFromJSON,
    JobBasicModelFromJSONTyped,
    JobBasicModelToJSON,
} from './JobBasicModel';
import type { WorkflowBasicModel } from './WorkflowBasicModel';
import {
    WorkflowBasicModelFromJSON,
    WorkflowBasicModelFromJSONTyped,
    WorkflowBasicModelToJSON,
} from './WorkflowBasicModel';

/**
 * Contains information about execution of a Integration workflow.
 * @export
 * @interface WorkflowExecutionBasicModel
 */
export interface WorkflowExecutionBasicModel {
    /**
     * The id of a workflow execution.
     * @type {number}
     * @memberof WorkflowExecutionBasicModel
     */
    readonly id?: number;
    /**
     * 
     * @type {IntegrationBasicModel}
     * @memberof WorkflowExecutionBasicModel
     */
    integration?: IntegrationBasicModel;
    /**
     * 
     * @type {IntegrationInstanceBasicModel}
     * @memberof WorkflowExecutionBasicModel
     */
    integrationInstance?: IntegrationInstanceBasicModel;
    /**
     * 
     * @type {JobBasicModel}
     * @memberof WorkflowExecutionBasicModel
     */
    job?: JobBasicModel;
    /**
     * 
     * @type {WorkflowBasicModel}
     * @memberof WorkflowExecutionBasicModel
     */
    workflow?: WorkflowBasicModel;
}

/**
 * Check if a given object implements the WorkflowExecutionBasicModel interface.
 */
export function instanceOfWorkflowExecutionBasicModel(value: object): boolean {
    let isInstance = true;

    return isInstance;
}

export function WorkflowExecutionBasicModelFromJSON(json: any): WorkflowExecutionBasicModel {
    return WorkflowExecutionBasicModelFromJSONTyped(json, false);
}

export function WorkflowExecutionBasicModelFromJSONTyped(json: any, ignoreDiscriminator: boolean): WorkflowExecutionBasicModel {
    if ((json === undefined) || (json === null)) {
        return json;
    }
    return {
        
        'id': !exists(json, 'id') ? undefined : json['id'],
        'integration': !exists(json, 'integration') ? undefined : IntegrationBasicModelFromJSON(json['integration']),
        'integrationInstance': !exists(json, 'integrationInstance') ? undefined : IntegrationInstanceBasicModelFromJSON(json['integrationInstance']),
        'job': !exists(json, 'job') ? undefined : JobBasicModelFromJSON(json['job']),
        'workflow': !exists(json, 'workflow') ? undefined : WorkflowBasicModelFromJSON(json['workflow']),
    };
}

export function WorkflowExecutionBasicModelToJSON(value?: WorkflowExecutionBasicModel | null): any {
    if (value === undefined) {
        return undefined;
    }
    if (value === null) {
        return null;
    }
    return {
        
        'integration': IntegrationBasicModelToJSON(value.integration),
        'integrationInstance': IntegrationInstanceBasicModelToJSON(value.integrationInstance),
        'job': JobBasicModelToJSON(value.job),
        'workflow': WorkflowBasicModelToJSON(value.workflow),
    };
}


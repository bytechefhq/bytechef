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
import type { IntegrationInstanceConfigurationBasicModel } from './IntegrationInstanceConfigurationBasicModel';
import {
    IntegrationInstanceConfigurationBasicModelFromJSON,
    IntegrationInstanceConfigurationBasicModelFromJSONTyped,
    IntegrationInstanceConfigurationBasicModelToJSON,
} from './IntegrationInstanceConfigurationBasicModel';
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
     * @type {IntegrationInstanceConfigurationBasicModel}
     * @memberof WorkflowExecutionBasicModel
     */
    integrationInstanceConfiguration?: IntegrationInstanceConfigurationBasicModel;
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
    return true;
}

export function WorkflowExecutionBasicModelFromJSON(json: any): WorkflowExecutionBasicModel {
    return WorkflowExecutionBasicModelFromJSONTyped(json, false);
}

export function WorkflowExecutionBasicModelFromJSONTyped(json: any, ignoreDiscriminator: boolean): WorkflowExecutionBasicModel {
    if (json == null) {
        return json;
    }
    return {
        
        'id': json['id'] == null ? undefined : json['id'],
        'integration': json['integration'] == null ? undefined : IntegrationBasicModelFromJSON(json['integration']),
        'integrationInstanceConfiguration': json['integrationInstanceConfiguration'] == null ? undefined : IntegrationInstanceConfigurationBasicModelFromJSON(json['integrationInstanceConfiguration']),
        'integrationInstance': json['integrationInstance'] == null ? undefined : IntegrationInstanceBasicModelFromJSON(json['integrationInstance']),
        'job': json['job'] == null ? undefined : JobBasicModelFromJSON(json['job']),
        'workflow': json['workflow'] == null ? undefined : WorkflowBasicModelFromJSON(json['workflow']),
    };
}

export function WorkflowExecutionBasicModelToJSON(value?: Omit<WorkflowExecutionBasicModel, 'id'> | null): any {
    if (value == null) {
        return value;
    }
    return {
        
        'integration': IntegrationBasicModelToJSON(value['integration']),
        'integrationInstanceConfiguration': IntegrationInstanceConfigurationBasicModelToJSON(value['integrationInstanceConfiguration']),
        'integrationInstance': IntegrationInstanceBasicModelToJSON(value['integrationInstance']),
        'job': JobBasicModelToJSON(value['job']),
        'workflow': WorkflowBasicModelToJSON(value['workflow']),
    };
}


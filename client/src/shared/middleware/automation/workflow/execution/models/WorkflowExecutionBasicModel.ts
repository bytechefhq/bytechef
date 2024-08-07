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
import type { JobBasicModel } from './JobBasicModel';
import {
    JobBasicModelFromJSON,
    JobBasicModelFromJSONTyped,
    JobBasicModelToJSON,
} from './JobBasicModel';
import type { ProjectBasicModel } from './ProjectBasicModel';
import {
    ProjectBasicModelFromJSON,
    ProjectBasicModelFromJSONTyped,
    ProjectBasicModelToJSON,
} from './ProjectBasicModel';
import type { ProjectInstanceBasicModel } from './ProjectInstanceBasicModel';
import {
    ProjectInstanceBasicModelFromJSON,
    ProjectInstanceBasicModelFromJSONTyped,
    ProjectInstanceBasicModelToJSON,
} from './ProjectInstanceBasicModel';
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
     * @type {ProjectBasicModel}
     * @memberof WorkflowExecutionBasicModel
     */
    project?: ProjectBasicModel;
    /**
     * 
     * @type {ProjectInstanceBasicModel}
     * @memberof WorkflowExecutionBasicModel
     */
    projectInstance?: ProjectInstanceBasicModel;
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
        'project': json['project'] == null ? undefined : ProjectBasicModelFromJSON(json['project']),
        'projectInstance': json['projectInstance'] == null ? undefined : ProjectInstanceBasicModelFromJSON(json['projectInstance']),
        'job': json['job'] == null ? undefined : JobBasicModelFromJSON(json['job']),
        'workflow': json['workflow'] == null ? undefined : WorkflowBasicModelFromJSON(json['workflow']),
    };
}

export function WorkflowExecutionBasicModelToJSON(value?: Omit<WorkflowExecutionBasicModel, 'id'> | null): any {
    if (value == null) {
        return value;
    }
    return {
        
        'project': ProjectBasicModelToJSON(value['project']),
        'projectInstance': ProjectInstanceBasicModelToJSON(value['projectInstance']),
        'job': JobBasicModelToJSON(value['job']),
        'workflow': WorkflowBasicModelToJSON(value['workflow']),
    };
}


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
import type { JobModel } from './JobModel';
import {
    JobModelFromJSON,
    JobModelFromJSONTyped,
    JobModelToJSON,
} from './JobModel';
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
import type { TriggerExecutionModel } from './TriggerExecutionModel';
import {
    TriggerExecutionModelFromJSON,
    TriggerExecutionModelFromJSONTyped,
    TriggerExecutionModelToJSON,
} from './TriggerExecutionModel';
import type { WorkflowBasicModel } from './WorkflowBasicModel';
import {
    WorkflowBasicModelFromJSON,
    WorkflowBasicModelFromJSONTyped,
    WorkflowBasicModelToJSON,
} from './WorkflowBasicModel';

/**
 * Contains information about execution of a project workflow.
 * @export
 * @interface WorkflowExecutionModel
 */
export interface WorkflowExecutionModel {
    /**
     * The id of a workflow execution.
     * @type {number}
     * @memberof WorkflowExecutionModel
     */
    readonly id?: number;
    /**
     * 
     * @type {JobModel}
     * @memberof WorkflowExecutionModel
     */
    job?: JobModel;
    /**
     * 
     * @type {ProjectBasicModel}
     * @memberof WorkflowExecutionModel
     */
    project?: ProjectBasicModel;
    /**
     * 
     * @type {ProjectInstanceBasicModel}
     * @memberof WorkflowExecutionModel
     */
    projectInstance?: ProjectInstanceBasicModel;
    /**
     * 
     * @type {TriggerExecutionModel}
     * @memberof WorkflowExecutionModel
     */
    triggerExecution?: TriggerExecutionModel;
    /**
     * 
     * @type {WorkflowBasicModel}
     * @memberof WorkflowExecutionModel
     */
    workflow?: WorkflowBasicModel;
}

/**
 * Check if a given object implements the WorkflowExecutionModel interface.
 */
export function instanceOfWorkflowExecutionModel(value: object): boolean {
    return true;
}

export function WorkflowExecutionModelFromJSON(json: any): WorkflowExecutionModel {
    return WorkflowExecutionModelFromJSONTyped(json, false);
}

export function WorkflowExecutionModelFromJSONTyped(json: any, ignoreDiscriminator: boolean): WorkflowExecutionModel {
    if (json == null) {
        return json;
    }
    return {
        
        'id': json['id'] == null ? undefined : json['id'],
        'job': json['job'] == null ? undefined : JobModelFromJSON(json['job']),
        'project': json['project'] == null ? undefined : ProjectBasicModelFromJSON(json['project']),
        'projectInstance': json['projectInstance'] == null ? undefined : ProjectInstanceBasicModelFromJSON(json['projectInstance']),
        'triggerExecution': json['triggerExecution'] == null ? undefined : TriggerExecutionModelFromJSON(json['triggerExecution']),
        'workflow': json['workflow'] == null ? undefined : WorkflowBasicModelFromJSON(json['workflow']),
    };
}

export function WorkflowExecutionModelToJSON(value?: Omit<WorkflowExecutionModel, 'id'> | null): any {
    if (value == null) {
        return value;
    }
    return {
        
        'job': JobModelToJSON(value['job']),
        'project': ProjectBasicModelToJSON(value['project']),
        'projectInstance': ProjectInstanceBasicModelToJSON(value['projectInstance']),
        'triggerExecution': TriggerExecutionModelToJSON(value['triggerExecution']),
        'workflow': WorkflowBasicModelToJSON(value['workflow']),
    };
}


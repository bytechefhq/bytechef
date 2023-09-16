/* tslint:disable */
/* eslint-disable */
/**
 * Project Execution API
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
import type { JobModel } from './JobModel';
import {
    JobModelFromJSON,
    JobModelFromJSONTyped,
    JobModelToJSON,
} from './JobModel';
import type { ProjectInstanceModel } from './ProjectInstanceModel';
import {
    ProjectInstanceModelFromJSON,
    ProjectInstanceModelFromJSONTyped,
    ProjectInstanceModelToJSON,
} from './ProjectInstanceModel';
import type { ProjectModel } from './ProjectModel';
import {
    ProjectModelFromJSON,
    ProjectModelFromJSONTyped,
    ProjectModelToJSON,
} from './ProjectModel';
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
     * @type {ProjectInstanceModel}
     * @memberof WorkflowExecutionModel
     */
    instance?: ProjectInstanceModel;
    /**
     * 
     * @type {JobModel}
     * @memberof WorkflowExecutionModel
     */
    job?: JobModel;
    /**
     * 
     * @type {ProjectModel}
     * @memberof WorkflowExecutionModel
     */
    project?: ProjectModel;
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
    let isInstance = true;

    return isInstance;
}

export function WorkflowExecutionModelFromJSON(json: any): WorkflowExecutionModel {
    return WorkflowExecutionModelFromJSONTyped(json, false);
}

export function WorkflowExecutionModelFromJSONTyped(json: any, ignoreDiscriminator: boolean): WorkflowExecutionModel {
    if ((json === undefined) || (json === null)) {
        return json;
    }
    return {
        
        'id': !exists(json, 'id') ? undefined : json['id'],
        'instance': !exists(json, 'instance') ? undefined : ProjectInstanceModelFromJSON(json['instance']),
        'job': !exists(json, 'job') ? undefined : JobModelFromJSON(json['job']),
        'project': !exists(json, 'project') ? undefined : ProjectModelFromJSON(json['project']),
        'workflow': !exists(json, 'workflow') ? undefined : WorkflowBasicModelFromJSON(json['workflow']),
    };
}

export function WorkflowExecutionModelToJSON(value?: WorkflowExecutionModel | null): any {
    if (value === undefined) {
        return undefined;
    }
    if (value === null) {
        return null;
    }
    return {
        
        'instance': ProjectInstanceModelToJSON(value.instance),
        'job': JobModelToJSON(value.job),
        'project': ProjectModelToJSON(value.project),
        'workflow': WorkflowBasicModelToJSON(value.workflow),
    };
}


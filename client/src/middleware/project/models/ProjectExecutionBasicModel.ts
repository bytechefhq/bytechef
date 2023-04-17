/* tslint:disable */
/* eslint-disable */
/**
 * Project API
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
 * Contains information about execution of one of project workflows.
 * @export
 * @interface ProjectExecutionBasicModel
 */
export interface ProjectExecutionBasicModel {
    /**
     * The id of a project execution.
     * @type {number}
     * @memberof ProjectExecutionBasicModel
     */
    readonly id?: number;
    /**
     * 
     * @type {ProjectInstanceBasicModel}
     * @memberof ProjectExecutionBasicModel
     */
    instance?: ProjectInstanceBasicModel;
    /**
     * 
     * @type {JobBasicModel}
     * @memberof ProjectExecutionBasicModel
     */
    job?: JobBasicModel;
    /**
     * 
     * @type {ProjectBasicModel}
     * @memberof ProjectExecutionBasicModel
     */
    project?: ProjectBasicModel;
    /**
     * 
     * @type {WorkflowBasicModel}
     * @memberof ProjectExecutionBasicModel
     */
    workflow?: WorkflowBasicModel;
}

/**
 * Check if a given object implements the ProjectExecutionBasicModel interface.
 */
export function instanceOfProjectExecutionBasicModel(value: object): boolean {
    let isInstance = true;

    return isInstance;
}

export function ProjectExecutionBasicModelFromJSON(json: any): ProjectExecutionBasicModel {
    return ProjectExecutionBasicModelFromJSONTyped(json, false);
}

export function ProjectExecutionBasicModelFromJSONTyped(json: any, ignoreDiscriminator: boolean): ProjectExecutionBasicModel {
    if ((json === undefined) || (json === null)) {
        return json;
    }
    return {
        
        'id': !exists(json, 'id') ? undefined : json['id'],
        'instance': !exists(json, 'instance') ? undefined : ProjectInstanceBasicModelFromJSON(json['instance']),
        'job': !exists(json, 'job') ? undefined : JobBasicModelFromJSON(json['job']),
        'project': !exists(json, 'project') ? undefined : ProjectBasicModelFromJSON(json['project']),
        'workflow': !exists(json, 'workflow') ? undefined : WorkflowBasicModelFromJSON(json['workflow']),
    };
}

export function ProjectExecutionBasicModelToJSON(value?: ProjectExecutionBasicModel | null): any {
    if (value === undefined) {
        return undefined;
    }
    if (value === null) {
        return null;
    }
    return {
        
        'instance': ProjectInstanceBasicModelToJSON(value.instance),
        'job': JobBasicModelToJSON(value.job),
        'project': ProjectBasicModelToJSON(value.project),
        'workflow': WorkflowBasicModelToJSON(value.workflow),
    };
}


/* tslint:disable */
/* eslint-disable */
/**
 * The Automation Configuration Internal API
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
import type { Environment } from './Environment';
import {
    EnvironmentFromJSON,
    EnvironmentFromJSONTyped,
    EnvironmentToJSON,
    EnvironmentToJSONTyped,
} from './Environment';
import type { Tag } from './Tag';
import {
    TagFromJSON,
    TagFromJSONTyped,
    TagToJSON,
    TagToJSONTyped,
} from './Tag';
import type { ProjectDeploymentWorkflow } from './ProjectDeploymentWorkflow';
import {
    ProjectDeploymentWorkflowFromJSON,
    ProjectDeploymentWorkflowFromJSONTyped,
    ProjectDeploymentWorkflowToJSON,
    ProjectDeploymentWorkflowToJSONTyped,
} from './ProjectDeploymentWorkflow';

/**
 * Contains configurations and connections required for the execution of project workflows.
 * @export
 * @interface ProjectDeployment
 */
export interface ProjectDeployment {
    /**
     * The created by.
     * @type {string}
     * @memberof ProjectDeployment
     */
    readonly createdBy?: string;
    /**
     * The created date.
     * @type {Date}
     * @memberof ProjectDeployment
     */
    readonly createdDate?: Date;
    /**
     * The description of a project deployment.
     * @type {string}
     * @memberof ProjectDeployment
     */
    description?: string;
    /**
     * If a project deployment is enabled or not.
     * @type {boolean}
     * @memberof ProjectDeployment
     */
    enabled?: boolean;
    /**
     * 
     * @type {Environment}
     * @memberof ProjectDeployment
     */
    environment?: Environment;
    /**
     * The id of a project deployment.
     * @type {number}
     * @memberof ProjectDeployment
     */
    readonly id?: number;
    /**
     * The last execution date.
     * @type {Date}
     * @memberof ProjectDeployment
     */
    readonly lastExecutionDate?: Date;
    /**
     * The last modified by.
     * @type {string}
     * @memberof ProjectDeployment
     */
    readonly lastModifiedBy?: string;
    /**
     * The last modified date.
     * @type {Date}
     * @memberof ProjectDeployment
     */
    readonly lastModifiedDate?: Date;
    /**
     * The name of a project deployment.
     * @type {string}
     * @memberof ProjectDeployment
     */
    name: string;
    /**
     * The id of a project.
     * @type {number}
     * @memberof ProjectDeployment
     */
    projectId?: number;
    /**
     * The version of a project.
     * @type {number}
     * @memberof ProjectDeployment
     */
    projectVersion?: number;
    /**
     * 
     * @type {object}
     * @memberof ProjectDeployment
     */
    readonly project?: object;
    /**
     * 
     * @type {Array<ProjectDeploymentWorkflow>}
     * @memberof ProjectDeployment
     */
    projectDeploymentWorkflows?: Array<ProjectDeploymentWorkflow>;
    /**
     * 
     * @type {Array<Tag>}
     * @memberof ProjectDeployment
     */
    tags?: Array<Tag>;
    /**
     * 
     * @type {number}
     * @memberof ProjectDeployment
     */
    version?: number;
}



/**
 * Check if a given object implements the ProjectDeployment interface.
 */
export function instanceOfProjectDeployment(value: object): value is ProjectDeployment {
    if (!('name' in value) || value['name'] === undefined) return false;
    return true;
}

export function ProjectDeploymentFromJSON(json: any): ProjectDeployment {
    return ProjectDeploymentFromJSONTyped(json, false);
}

export function ProjectDeploymentFromJSONTyped(json: any, ignoreDiscriminator: boolean): ProjectDeployment {
    if (json == null) {
        return json;
    }
    return {
        
        'createdBy': json['createdBy'] == null ? undefined : json['createdBy'],
        'createdDate': json['createdDate'] == null ? undefined : (new Date(json['createdDate'])),
        'description': json['description'] == null ? undefined : json['description'],
        'enabled': json['enabled'] == null ? undefined : json['enabled'],
        'environment': json['environment'] == null ? undefined : EnvironmentFromJSON(json['environment']),
        'id': json['id'] == null ? undefined : json['id'],
        'lastExecutionDate': json['lastExecutionDate'] == null ? undefined : (new Date(json['lastExecutionDate'])),
        'lastModifiedBy': json['lastModifiedBy'] == null ? undefined : json['lastModifiedBy'],
        'lastModifiedDate': json['lastModifiedDate'] == null ? undefined : (new Date(json['lastModifiedDate'])),
        'name': json['name'],
        'projectId': json['projectId'] == null ? undefined : json['projectId'],
        'projectVersion': json['projectVersion'] == null ? undefined : json['projectVersion'],
        'project': json['project'] == null ? undefined : json['project'],
        'projectDeploymentWorkflows': json['projectDeploymentWorkflows'] == null ? undefined : ((json['projectDeploymentWorkflows'] as Array<any>).map(ProjectDeploymentWorkflowFromJSON)),
        'tags': json['tags'] == null ? undefined : ((json['tags'] as Array<any>).map(TagFromJSON)),
        'version': json['__version'] == null ? undefined : json['__version'],
    };
}

export function ProjectDeploymentToJSON(json: any): ProjectDeployment {
    return ProjectDeploymentToJSONTyped(json, false);
}

export function ProjectDeploymentToJSONTyped(value?: Omit<ProjectDeployment, 'createdBy'|'createdDate'|'id'|'lastExecutionDate'|'lastModifiedBy'|'lastModifiedDate'|'project'> | null, ignoreDiscriminator: boolean = false): any {
    if (value == null) {
        return value;
    }

    return {
        
        'description': value['description'],
        'enabled': value['enabled'],
        'environment': EnvironmentToJSON(value['environment']),
        'name': value['name'],
        'projectId': value['projectId'],
        'projectVersion': value['projectVersion'],
        'projectDeploymentWorkflows': value['projectDeploymentWorkflows'] == null ? undefined : ((value['projectDeploymentWorkflows'] as Array<any>).map(ProjectDeploymentWorkflowToJSON)),
        'tags': value['tags'] == null ? undefined : ((value['tags'] as Array<any>).map(TagToJSON)),
        '__version': value['version'],
    };
}

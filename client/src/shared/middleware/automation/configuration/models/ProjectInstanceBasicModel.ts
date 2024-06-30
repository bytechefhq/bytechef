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
import type { EnvironmentModel } from './EnvironmentModel';
import {
    EnvironmentModelFromJSON,
    EnvironmentModelFromJSONTyped,
    EnvironmentModelToJSON,
} from './EnvironmentModel';

/**
 * Contains configurations and connections required for the execution of project workflows.
 * @export
 * @interface ProjectInstanceBasicModel
 */
export interface ProjectInstanceBasicModel {
    /**
     * The description of a project instance.
     * @type {string}
     * @memberof ProjectInstanceBasicModel
     */
    description?: string;
    /**
     * The created by.
     * @type {string}
     * @memberof ProjectInstanceBasicModel
     */
    readonly createdBy?: string;
    /**
     * The created date.
     * @type {Date}
     * @memberof ProjectInstanceBasicModel
     */
    readonly createdDate?: Date;
    /**
     * If a project instance is enabled or not.
     * @type {boolean}
     * @memberof ProjectInstanceBasicModel
     */
    enabled?: boolean;
    /**
     * 
     * @type {EnvironmentModel}
     * @memberof ProjectInstanceBasicModel
     */
    environment?: EnvironmentModel;
    /**
     * The id of a project instance.
     * @type {number}
     * @memberof ProjectInstanceBasicModel
     */
    readonly id?: number;
    /**
     * The last execution date.
     * @type {Date}
     * @memberof ProjectInstanceBasicModel
     */
    readonly lastExecutionDate?: Date;
    /**
     * The last modified by.
     * @type {string}
     * @memberof ProjectInstanceBasicModel
     */
    readonly lastModifiedBy?: string;
    /**
     * The last modified date.
     * @type {Date}
     * @memberof ProjectInstanceBasicModel
     */
    readonly lastModifiedDate?: Date;
    /**
     * The name of a project instance.
     * @type {string}
     * @memberof ProjectInstanceBasicModel
     */
    name: string;
    /**
     * The id of a project.
     * @type {number}
     * @memberof ProjectInstanceBasicModel
     */
    projectId?: number;
    /**
     * The version of a project.
     * @type {number}
     * @memberof ProjectInstanceBasicModel
     */
    projectVersion?: number;
}

/**
 * Check if a given object implements the ProjectInstanceBasicModel interface.
 */
export function instanceOfProjectInstanceBasicModel(value: object): boolean {
    if (!('name' in value)) return false;
    return true;
}

export function ProjectInstanceBasicModelFromJSON(json: any): ProjectInstanceBasicModel {
    return ProjectInstanceBasicModelFromJSONTyped(json, false);
}

export function ProjectInstanceBasicModelFromJSONTyped(json: any, ignoreDiscriminator: boolean): ProjectInstanceBasicModel {
    if (json == null) {
        return json;
    }
    return {
        
        'description': json['description'] == null ? undefined : json['description'],
        'createdBy': json['createdBy'] == null ? undefined : json['createdBy'],
        'createdDate': json['createdDate'] == null ? undefined : (new Date(json['createdDate'])),
        'enabled': json['enabled'] == null ? undefined : json['enabled'],
        'environment': json['environment'] == null ? undefined : EnvironmentModelFromJSON(json['environment']),
        'id': json['id'] == null ? undefined : json['id'],
        'lastExecutionDate': json['lastExecutionDate'] == null ? undefined : (new Date(json['lastExecutionDate'])),
        'lastModifiedBy': json['lastModifiedBy'] == null ? undefined : json['lastModifiedBy'],
        'lastModifiedDate': json['lastModifiedDate'] == null ? undefined : (new Date(json['lastModifiedDate'])),
        'name': json['name'],
        'projectId': json['projectId'] == null ? undefined : json['projectId'],
        'projectVersion': json['projectVersion'] == null ? undefined : json['projectVersion'],
    };
}

export function ProjectInstanceBasicModelToJSON(value?: Omit<ProjectInstanceBasicModel, 'createdBy'|'createdDate'|'id'|'lastExecutionDate'|'lastModifiedBy'|'lastModifiedDate'> | null): any {
    if (value == null) {
        return value;
    }
    return {
        
        'description': value['description'],
        'enabled': value['enabled'],
        'environment': EnvironmentModelToJSON(value['environment']),
        'name': value['name'],
        'projectId': value['projectId'],
        'projectVersion': value['projectVersion'],
    };
}


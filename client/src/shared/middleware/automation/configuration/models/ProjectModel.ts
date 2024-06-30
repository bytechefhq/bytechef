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
import type { CategoryModel } from './CategoryModel';
import {
    CategoryModelFromJSON,
    CategoryModelFromJSONTyped,
    CategoryModelToJSON,
} from './CategoryModel';
import type { ProjectStatusModel } from './ProjectStatusModel';
import {
    ProjectStatusModelFromJSON,
    ProjectStatusModelFromJSONTyped,
    ProjectStatusModelToJSON,
} from './ProjectStatusModel';
import type { TagModel } from './TagModel';
import {
    TagModelFromJSON,
    TagModelFromJSONTyped,
    TagModelToJSON,
} from './TagModel';

/**
 * A group of workflows that make one logical project.
 * @export
 * @interface ProjectModel
 */
export interface ProjectModel {
    /**
     * 
     * @type {CategoryModel}
     * @memberof ProjectModel
     */
    category?: CategoryModel;
    /**
     * The created by.
     * @type {string}
     * @memberof ProjectModel
     */
    readonly createdBy?: string;
    /**
     * The created date.
     * @type {Date}
     * @memberof ProjectModel
     */
    readonly createdDate?: Date;
    /**
     * The description of a project.
     * @type {string}
     * @memberof ProjectModel
     */
    description?: string;
    /**
     * The id of a project.
     * @type {number}
     * @memberof ProjectModel
     */
    readonly id?: number;
    /**
     * The last modified by.
     * @type {string}
     * @memberof ProjectModel
     */
    readonly lastModifiedBy?: string;
    /**
     * The last modified date.
     * @type {Date}
     * @memberof ProjectModel
     */
    readonly lastModifiedDate?: Date;
    /**
     * The name of a project.
     * @type {string}
     * @memberof ProjectModel
     */
    name: string;
    /**
     * The published date.
     * @type {Date}
     * @memberof ProjectModel
     */
    publishedDate?: Date;
    /**
     * The version of a project.
     * @type {number}
     * @memberof ProjectModel
     */
    readonly projectVersion?: number;
    /**
     * The project workflow ids belonging to this project.
     * @type {Array<string>}
     * @memberof ProjectModel
     */
    projectWorkflowIds?: Array<string>;
    /**
     * 
     * @type {ProjectStatusModel}
     * @memberof ProjectModel
     */
    status?: ProjectStatusModel;
    /**
     * 
     * @type {Array<TagModel>}
     * @memberof ProjectModel
     */
    tags?: Array<TagModel>;
    /**
     * The workspace id.
     * @type {number}
     * @memberof ProjectModel
     */
    workspaceId: number;
    /**
     * 
     * @type {number}
     * @memberof ProjectModel
     */
    version?: number;
}

/**
 * Check if a given object implements the ProjectModel interface.
 */
export function instanceOfProjectModel(value: object): boolean {
    if (!('name' in value)) return false;
    if (!('workspaceId' in value)) return false;
    return true;
}

export function ProjectModelFromJSON(json: any): ProjectModel {
    return ProjectModelFromJSONTyped(json, false);
}

export function ProjectModelFromJSONTyped(json: any, ignoreDiscriminator: boolean): ProjectModel {
    if (json == null) {
        return json;
    }
    return {
        
        'category': json['category'] == null ? undefined : CategoryModelFromJSON(json['category']),
        'createdBy': json['createdBy'] == null ? undefined : json['createdBy'],
        'createdDate': json['createdDate'] == null ? undefined : (new Date(json['createdDate'])),
        'description': json['description'] == null ? undefined : json['description'],
        'id': json['id'] == null ? undefined : json['id'],
        'lastModifiedBy': json['lastModifiedBy'] == null ? undefined : json['lastModifiedBy'],
        'lastModifiedDate': json['lastModifiedDate'] == null ? undefined : (new Date(json['lastModifiedDate'])),
        'name': json['name'],
        'publishedDate': json['publishedDate'] == null ? undefined : (new Date(json['publishedDate'])),
        'projectVersion': json['projectVersion'] == null ? undefined : json['projectVersion'],
        'projectWorkflowIds': json['projectWorkflowIds'] == null ? undefined : json['projectWorkflowIds'],
        'status': json['status'] == null ? undefined : ProjectStatusModelFromJSON(json['status']),
        'tags': json['tags'] == null ? undefined : ((json['tags'] as Array<any>).map(TagModelFromJSON)),
        'workspaceId': json['workspaceId'],
        'version': json['__version'] == null ? undefined : json['__version'],
    };
}

export function ProjectModelToJSON(value?: Omit<ProjectModel, 'createdBy'|'createdDate'|'id'|'lastModifiedBy'|'lastModifiedDate'|'projectVersion'> | null): any {
    if (value == null) {
        return value;
    }
    return {
        
        'category': CategoryModelToJSON(value['category']),
        'description': value['description'],
        'name': value['name'],
        'publishedDate': value['publishedDate'] == null ? undefined : ((value['publishedDate']).toISOString()),
        'projectWorkflowIds': value['projectWorkflowIds'],
        'status': ProjectStatusModelToJSON(value['status']),
        'tags': value['tags'] == null ? undefined : ((value['tags'] as Array<any>).map(TagModelToJSON)),
        'workspaceId': value['workspaceId'],
        '__version': value['version'],
    };
}


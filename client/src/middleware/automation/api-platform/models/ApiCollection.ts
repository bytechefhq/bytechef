/* tslint:disable */
/* eslint-disable */
/**
 * The Automation API Platform Internal API
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
import type { ApiCollectionEndpoint } from './ApiCollectionEndpoint';
import {
    ApiCollectionEndpointFromJSON,
    ApiCollectionEndpointFromJSONTyped,
    ApiCollectionEndpointToJSON,
    ApiCollectionEndpointToJSONTyped,
} from './ApiCollectionEndpoint';
import type { ProjectInstanceBasic } from './ProjectInstanceBasic';
import {
    ProjectInstanceBasicFromJSON,
    ProjectInstanceBasicFromJSONTyped,
    ProjectInstanceBasicToJSON,
    ProjectInstanceBasicToJSONTyped,
} from './ProjectInstanceBasic';
import type { Tag } from './Tag';
import {
    TagFromJSON,
    TagFromJSONTyped,
    TagToJSON,
    TagToJSONTyped,
} from './Tag';
import type { ProjectBasic } from './ProjectBasic';
import {
    ProjectBasicFromJSON,
    ProjectBasicFromJSONTyped,
    ProjectBasicToJSON,
    ProjectBasicToJSONTyped,
} from './ProjectBasic';

/**
 * An API collection.
 * @export
 * @interface ApiCollection
 */
export interface ApiCollection {
    /**
     * The version of an API collection.
     * @type {number}
     * @memberof ApiCollection
     */
    collectionVersion?: number;
    /**
     * The created by.
     * @type {string}
     * @memberof ApiCollection
     */
    readonly createdBy?: string;
    /**
     * The created date.
     * @type {Date}
     * @memberof ApiCollection
     */
    readonly createdDate?: Date;
    /**
     * The description of an API collection.
     * @type {string}
     * @memberof ApiCollection
     */
    description?: string;
    /**
     * If an API collection is enabled or not.
     * @type {boolean}
     * @memberof ApiCollection
     */
    enabled: boolean;
    /**
     * 
     * @type {Array<ApiCollectionEndpoint>}
     * @memberof ApiCollection
     */
    endpoints?: Array<ApiCollectionEndpoint>;
    /**
     * The id of an API collection.
     * @type {number}
     * @memberof ApiCollection
     */
    readonly id?: number;
    /**
     * The name of an API collection.
     * @type {string}
     * @memberof ApiCollection
     */
    name: string;
    /**
     * The last modified by.
     * @type {string}
     * @memberof ApiCollection
     */
    readonly lastModifiedBy?: string;
    /**
     * The last modified date.
     * @type {Date}
     * @memberof ApiCollection
     */
    readonly lastModifiedDate?: Date;
    /**
     * The id of a project the API collection is connected to.
     * @type {number}
     * @memberof ApiCollection
     */
    projectId: number;
    /**
     * 
     * @type {ProjectBasic}
     * @memberof ApiCollection
     */
    project?: ProjectBasic;
    /**
     * The id of an project instance the API collection is connected to.
     * @type {number}
     * @memberof ApiCollection
     */
    readonly projectInstanceId?: number;
    /**
     * 
     * @type {ProjectInstanceBasic}
     * @memberof ApiCollection
     */
    projectInstance?: ProjectInstanceBasic;
    /**
     * The version of a project the API collection is connected to.
     * @type {number}
     * @memberof ApiCollection
     */
    projectVersion: number;
    /**
     * 
     * @type {Array<Tag>}
     * @memberof ApiCollection
     */
    tags?: Array<Tag>;
    /**
     * The workspace id.
     * @type {number}
     * @memberof ApiCollection
     */
    workspaceId: number;
    /**
     * 
     * @type {number}
     * @memberof ApiCollection
     */
    version?: number;
}

/**
 * Check if a given object implements the ApiCollection interface.
 */
export function instanceOfApiCollection(value: object): value is ApiCollection {
    if (!('enabled' in value) || value['enabled'] === undefined) return false;
    if (!('name' in value) || value['name'] === undefined) return false;
    if (!('projectId' in value) || value['projectId'] === undefined) return false;
    if (!('projectVersion' in value) || value['projectVersion'] === undefined) return false;
    if (!('workspaceId' in value) || value['workspaceId'] === undefined) return false;
    return true;
}

export function ApiCollectionFromJSON(json: any): ApiCollection {
    return ApiCollectionFromJSONTyped(json, false);
}

export function ApiCollectionFromJSONTyped(json: any, ignoreDiscriminator: boolean): ApiCollection {
    if (json == null) {
        return json;
    }
    return {
        
        'collectionVersion': json['collectionVersion'] == null ? undefined : json['collectionVersion'],
        'createdBy': json['createdBy'] == null ? undefined : json['createdBy'],
        'createdDate': json['createdDate'] == null ? undefined : (new Date(json['createdDate'])),
        'description': json['description'] == null ? undefined : json['description'],
        'enabled': json['enabled'],
        'endpoints': json['endpoints'] == null ? undefined : ((json['endpoints'] as Array<any>).map(ApiCollectionEndpointFromJSON)),
        'id': json['id'] == null ? undefined : json['id'],
        'name': json['name'],
        'lastModifiedBy': json['lastModifiedBy'] == null ? undefined : json['lastModifiedBy'],
        'lastModifiedDate': json['lastModifiedDate'] == null ? undefined : (new Date(json['lastModifiedDate'])),
        'projectId': json['projectId'],
        'project': json['project'] == null ? undefined : ProjectBasicFromJSON(json['project']),
        'projectInstanceId': json['projectInstanceId'] == null ? undefined : json['projectInstanceId'],
        'projectInstance': json['projectInstance'] == null ? undefined : ProjectInstanceBasicFromJSON(json['projectInstance']),
        'projectVersion': json['projectVersion'],
        'tags': json['tags'] == null ? undefined : ((json['tags'] as Array<any>).map(TagFromJSON)),
        'workspaceId': json['workspaceId'],
        'version': json['__version'] == null ? undefined : json['__version'],
    };
}

export function ApiCollectionToJSON(json: any): ApiCollection {
    return ApiCollectionToJSONTyped(json, false);
}

export function ApiCollectionToJSONTyped(value?: Omit<ApiCollection, 'createdBy'|'createdDate'|'id'|'lastModifiedBy'|'lastModifiedDate'|'projectInstanceId'> | null, ignoreDiscriminator: boolean = false): any {
    if (value == null) {
        return value;
    }

    return {
        
        'collectionVersion': value['collectionVersion'],
        'description': value['description'],
        'enabled': value['enabled'],
        'endpoints': value['endpoints'] == null ? undefined : ((value['endpoints'] as Array<any>).map(ApiCollectionEndpointToJSON)),
        'name': value['name'],
        'projectId': value['projectId'],
        'project': ProjectBasicToJSON(value['project']),
        'projectInstance': ProjectInstanceBasicToJSON(value['projectInstance']),
        'projectVersion': value['projectVersion'],
        'tags': value['tags'] == null ? undefined : ((value['tags'] as Array<any>).map(TagToJSON)),
        'workspaceId': value['workspaceId'],
        '__version': value['version'],
    };
}


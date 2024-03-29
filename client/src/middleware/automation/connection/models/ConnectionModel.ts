/* tslint:disable */
/* eslint-disable */
/**
 * The Automation Connection API
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
import type { CredentialStatusModel } from './CredentialStatusModel';
import {
    CredentialStatusModelFromJSON,
    CredentialStatusModelFromJSONTyped,
    CredentialStatusModelToJSON,
} from './CredentialStatusModel';
import type { TagModel } from './TagModel';
import {
    TagModelFromJSON,
    TagModelFromJSONTyped,
    TagModelToJSON,
} from './TagModel';

/**
 * Contains all required information to open a connection to a service defined by componentName parameter.
 * @export
 * @interface ConnectionModel
 */
export interface ConnectionModel {
    /**
     * If a connection is used in any of active workflows.
     * @type {boolean}
     * @memberof ConnectionModel
     */
    readonly active?: boolean;
    /**
     * The name of an authorization used by this connection. Used for HTTP based services.
     * @type {string}
     * @memberof ConnectionModel
     */
    authorizationName?: string;
    /**
     * The name of a component that uses this connection.
     * @type {string}
     * @memberof ConnectionModel
     */
    componentName: string;
    /**
     * The version of a component that uses this connection.
     * @type {number}
     * @memberof ConnectionModel
     */
    connectionVersion?: number;
    /**
     * The created by.
     * @type {string}
     * @memberof ConnectionModel
     */
    readonly createdBy?: string;
    /**
     * The created date.
     * @type {Date}
     * @memberof ConnectionModel
     */
    readonly createdDate?: Date;
    /**
     * 
     * @type {CredentialStatusModel}
     * @memberof ConnectionModel
     */
    credentialStatus?: CredentialStatusModel;
    /**
     * The name of a connection.
     * @type {string}
     * @memberof ConnectionModel
     */
    name: string;
    /**
     * The id of a connection.
     * @type {number}
     * @memberof ConnectionModel
     */
    readonly id?: number;
    /**
     * The last modified by.
     * @type {string}
     * @memberof ConnectionModel
     */
    readonly lastModifiedBy?: string;
    /**
     * The last modified date.
     * @type {Date}
     * @memberof ConnectionModel
     */
    readonly lastModifiedDate?: Date;
    /**
     * The parameters of a connection.
     * @type {{ [key: string]: object; }}
     * @memberof ConnectionModel
     */
    parameters: { [key: string]: object; };
    /**
     * 
     * @type {Array<TagModel>}
     * @memberof ConnectionModel
     */
    tags?: Array<TagModel>;
    /**
     * 
     * @type {number}
     * @memberof ConnectionModel
     */
    version?: number;
}

/**
 * Check if a given object implements the ConnectionModel interface.
 */
export function instanceOfConnectionModel(value: object): boolean {
    let isInstance = true;
    isInstance = isInstance && "componentName" in value;
    isInstance = isInstance && "name" in value;
    isInstance = isInstance && "parameters" in value;

    return isInstance;
}

export function ConnectionModelFromJSON(json: any): ConnectionModel {
    return ConnectionModelFromJSONTyped(json, false);
}

export function ConnectionModelFromJSONTyped(json: any, ignoreDiscriminator: boolean): ConnectionModel {
    if ((json === undefined) || (json === null)) {
        return json;
    }
    return {
        
        'active': !exists(json, 'active') ? undefined : json['active'],
        'authorizationName': !exists(json, 'authorizationName') ? undefined : json['authorizationName'],
        'componentName': json['componentName'],
        'connectionVersion': !exists(json, 'connectionVersion') ? undefined : json['connectionVersion'],
        'createdBy': !exists(json, 'createdBy') ? undefined : json['createdBy'],
        'createdDate': !exists(json, 'createdDate') ? undefined : (new Date(json['createdDate'])),
        'credentialStatus': !exists(json, 'credentialStatus') ? undefined : CredentialStatusModelFromJSON(json['credentialStatus']),
        'name': json['name'],
        'id': !exists(json, 'id') ? undefined : json['id'],
        'lastModifiedBy': !exists(json, 'lastModifiedBy') ? undefined : json['lastModifiedBy'],
        'lastModifiedDate': !exists(json, 'lastModifiedDate') ? undefined : (new Date(json['lastModifiedDate'])),
        'parameters': json['parameters'],
        'tags': !exists(json, 'tags') ? undefined : ((json['tags'] as Array<any>).map(TagModelFromJSON)),
        'version': !exists(json, '__version') ? undefined : json['__version'],
    };
}

export function ConnectionModelToJSON(value?: ConnectionModel | null): any {
    if (value === undefined) {
        return undefined;
    }
    if (value === null) {
        return null;
    }
    return {
        
        'authorizationName': value.authorizationName,
        'componentName': value.componentName,
        'connectionVersion': value.connectionVersion,
        'credentialStatus': CredentialStatusModelToJSON(value.credentialStatus),
        'name': value.name,
        'parameters': value.parameters,
        'tags': value.tags === undefined ? undefined : ((value.tags as Array<any>).map(TagModelToJSON)),
        '__version': value.version,
    };
}


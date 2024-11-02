/* tslint:disable */
/* eslint-disable */
/**
 * The Embedded Connected User Internal API
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
import type { ConnectedUserIntegrationInstance } from './ConnectedUserIntegrationInstance';
import {
    ConnectedUserIntegrationInstanceFromJSON,
    ConnectedUserIntegrationInstanceFromJSONTyped,
    ConnectedUserIntegrationInstanceToJSON,
    ConnectedUserIntegrationInstanceToJSONTyped,
} from './ConnectedUserIntegrationInstance';
import type { Environment } from './Environment';
import {
    EnvironmentFromJSON,
    EnvironmentFromJSONTyped,
    EnvironmentToJSON,
    EnvironmentToJSONTyped,
} from './Environment';

/**
 * 
 * @export
 * @interface ConnectedUser
 */
export interface ConnectedUser {
    /**
     * The created by.
     * @type {string}
     * @memberof ConnectedUser
     */
    readonly createdBy?: string;
    /**
     * The created date.
     * @type {Date}
     * @memberof ConnectedUser
     */
    readonly createdDate?: Date;
    /**
     * The email address.
     * @type {string}
     * @memberof ConnectedUser
     */
    email?: string;
    /**
     * If a connected user is enabled or not
     * @type {boolean}
     * @memberof ConnectedUser
     */
    enabled?: boolean;
    /**
     * 
     * @type {Environment}
     * @memberof ConnectedUser
     */
    environment?: Environment;
    /**
     * The connected user external id.
     * @type {string}
     * @memberof ConnectedUser
     */
    readonly externalId: string;
    /**
     * The id of a connected user.
     * @type {number}
     * @memberof ConnectedUser
     */
    readonly id?: number;
    /**
     * 
     * @type {Array<ConnectedUserIntegrationInstance>}
     * @memberof ConnectedUser
     */
    integrationInstances?: Array<ConnectedUserIntegrationInstance>;
    /**
     * 
     * @type {{ [key: string]: any; }}
     * @memberof ConnectedUser
     */
    readonly metadata?: { [key: string]: any; };
    /**
     * The name of a connection.
     * @type {string}
     * @memberof ConnectedUser
     */
    name?: string;
    /**
     * The last modified by.
     * @type {string}
     * @memberof ConnectedUser
     */
    readonly lastModifiedBy?: string;
    /**
     * The last modified date.
     * @type {Date}
     * @memberof ConnectedUser
     */
    readonly lastModifiedDate?: Date;
    /**
     * 
     * @type {number}
     * @memberof ConnectedUser
     */
    version?: number;
}



/**
 * Check if a given object implements the ConnectedUser interface.
 */
export function instanceOfConnectedUser(value: object): value is ConnectedUser {
    if (!('externalId' in value) || value['externalId'] === undefined) return false;
    return true;
}

export function ConnectedUserFromJSON(json: any): ConnectedUser {
    return ConnectedUserFromJSONTyped(json, false);
}

export function ConnectedUserFromJSONTyped(json: any, ignoreDiscriminator: boolean): ConnectedUser {
    if (json == null) {
        return json;
    }
    return {
        
        'createdBy': json['createdBy'] == null ? undefined : json['createdBy'],
        'createdDate': json['createdDate'] == null ? undefined : (new Date(json['createdDate'])),
        'email': json['email'] == null ? undefined : json['email'],
        'enabled': json['enabled'] == null ? undefined : json['enabled'],
        'environment': json['environment'] == null ? undefined : EnvironmentFromJSON(json['environment']),
        'externalId': json['externalId'],
        'id': json['id'] == null ? undefined : json['id'],
        'integrationInstances': json['integrationInstances'] == null ? undefined : ((json['integrationInstances'] as Array<any>).map(ConnectedUserIntegrationInstanceFromJSON)),
        'metadata': json['metadata'] == null ? undefined : json['metadata'],
        'name': json['name'] == null ? undefined : json['name'],
        'lastModifiedBy': json['lastModifiedBy'] == null ? undefined : json['lastModifiedBy'],
        'lastModifiedDate': json['lastModifiedDate'] == null ? undefined : (new Date(json['lastModifiedDate'])),
        'version': json['__version'] == null ? undefined : json['__version'],
    };
}

  export function ConnectedUserToJSON(json: any): ConnectedUser {
      return ConnectedUserToJSONTyped(json, false);
  }

  export function ConnectedUserToJSONTyped(value?: Omit<ConnectedUser, 'createdBy'|'createdDate'|'externalId'|'id'|'metadata'|'lastModifiedBy'|'lastModifiedDate'> | null, ignoreDiscriminator: boolean = false): any {
    if (value == null) {
        return value;
    }

    return {
        
        'email': value['email'],
        'enabled': value['enabled'],
        'environment': EnvironmentToJSON(value['environment']),
        'integrationInstances': value['integrationInstances'] == null ? undefined : ((value['integrationInstances'] as Array<any>).map(ConnectedUserIntegrationInstanceToJSON)),
        'name': value['name'],
        '__version': value['version'],
    };
}


/* tslint:disable */
/* eslint-disable */
/**
 * The Platform Connection Internal API
 * No description provided (generated by Openapi Generator https://github.com/openapitools/openapi-generator)
 *
 * The version of the OpenAPI document: 1
 * 
 *
 * NOTE: This class is auto generated by OpenAPI Generator (https://openapi-generator.tech).
 * https://openapi-generator.tech
 * Do not edit the class manually.
 */


/**
 * The environment of a connection.
 * @export
 */
export const ConnectionEnvironment = {
    Development: 'DEVELOPMENT',
    Test: 'TEST',
    Production: 'PRODUCTION'
} as const;
export type ConnectionEnvironment = typeof ConnectionEnvironment[keyof typeof ConnectionEnvironment];


export function instanceOfConnectionEnvironment(value: any): boolean {
    for (const key in ConnectionEnvironment) {
        if (Object.prototype.hasOwnProperty.call(ConnectionEnvironment, key)) {
            if (ConnectionEnvironment[key as keyof typeof ConnectionEnvironment] === value) {
                return true;
            }
        }
    }
    return false;
}

export function ConnectionEnvironmentFromJSON(json: any): ConnectionEnvironment {
    return ConnectionEnvironmentFromJSONTyped(json, false);
}

export function ConnectionEnvironmentFromJSONTyped(json: any, ignoreDiscriminator: boolean): ConnectionEnvironment {
    return json as ConnectionEnvironment;
}

export function ConnectionEnvironmentToJSON(value?: ConnectionEnvironment | null): any {
    return value as any;
}


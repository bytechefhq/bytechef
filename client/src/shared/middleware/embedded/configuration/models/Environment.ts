/* tslint:disable */
/* eslint-disable */
/**
 * The Embedded Configuration Internal API
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
 * The environment of a project.
 * @export
 */
export const Environment = {
    Test: 'TEST',
    Production: 'PRODUCTION'
} as const;
export type Environment = typeof Environment[keyof typeof Environment];


export function instanceOfEnvironment(value: any): boolean {
    for (const key in Environment) {
        if (Object.prototype.hasOwnProperty.call(Environment, key)) {
            if (Environment[key as keyof typeof Environment] === value) {
                return true;
            }
        }
    }
    return false;
}

export function EnvironmentFromJSON(json: any): Environment {
    return EnvironmentFromJSONTyped(json, false);
}

export function EnvironmentFromJSONTyped(json: any, ignoreDiscriminator: boolean): Environment {
    return json as Environment;
}

export function EnvironmentToJSON(value?: Environment | null): any {
    return value as any;
}


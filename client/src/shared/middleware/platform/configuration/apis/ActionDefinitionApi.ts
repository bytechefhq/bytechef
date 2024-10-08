/* tslint:disable */
/* eslint-disable */
/**
 * The Platform Configuration Internal API
 * No description provided (generated by Openapi Generator https://github.com/openapitools/openapi-generator)
 *
 * The version of the OpenAPI document: 1
 * 
 *
 * NOTE: This class is auto generated by OpenAPI Generator (https://openapi-generator.tech).
 * https://openapi-generator.tech
 * Do not edit the class manually.
 */


import * as runtime from '../runtime';
import type {
  ActionDefinition,
  ActionDefinitionBasic,
} from '../models/index';
import {
    ActionDefinitionFromJSON,
    ActionDefinitionToJSON,
    ActionDefinitionBasicFromJSON,
    ActionDefinitionBasicToJSON,
} from '../models/index';

export interface GetComponentActionDefinitionRequest {
    componentName: string;
    componentVersion: number;
    actionName: string;
}

export interface GetComponentActionDefinitionsRequest {
    componentName: string;
    componentVersion: number;
}

/**
 * 
 */
export class ActionDefinitionApi extends runtime.BaseAPI {

    /**
     * Get an action definition of a component.
     * Get an action definition of a component
     */
    async getComponentActionDefinitionRaw(requestParameters: GetComponentActionDefinitionRequest, initOverrides?: RequestInit | runtime.InitOverrideFunction): Promise<runtime.ApiResponse<ActionDefinition>> {
        if (requestParameters['componentName'] == null) {
            throw new runtime.RequiredError(
                'componentName',
                'Required parameter "componentName" was null or undefined when calling getComponentActionDefinition().'
            );
        }

        if (requestParameters['componentVersion'] == null) {
            throw new runtime.RequiredError(
                'componentVersion',
                'Required parameter "componentVersion" was null or undefined when calling getComponentActionDefinition().'
            );
        }

        if (requestParameters['actionName'] == null) {
            throw new runtime.RequiredError(
                'actionName',
                'Required parameter "actionName" was null or undefined when calling getComponentActionDefinition().'
            );
        }

        const queryParameters: any = {};

        const headerParameters: runtime.HTTPHeaders = {};

        const response = await this.request({
            path: `/component-definitions/{componentName}/versions/{componentVersion}/action-definitions/{actionName}`.replace(`{${"componentName"}}`, encodeURIComponent(String(requestParameters['componentName']))).replace(`{${"componentVersion"}}`, encodeURIComponent(String(requestParameters['componentVersion']))).replace(`{${"actionName"}}`, encodeURIComponent(String(requestParameters['actionName']))),
            method: 'GET',
            headers: headerParameters,
            query: queryParameters,
        }, initOverrides);

        return new runtime.JSONApiResponse(response, (jsonValue) => ActionDefinitionFromJSON(jsonValue));
    }

    /**
     * Get an action definition of a component.
     * Get an action definition of a component
     */
    async getComponentActionDefinition(requestParameters: GetComponentActionDefinitionRequest, initOverrides?: RequestInit | runtime.InitOverrideFunction): Promise<ActionDefinition> {
        const response = await this.getComponentActionDefinitionRaw(requestParameters, initOverrides);
        return await response.value();
    }

    /**
     * Get a list of action definitions for a component.
     * Get a list of action definitions for a component
     */
    async getComponentActionDefinitionsRaw(requestParameters: GetComponentActionDefinitionsRequest, initOverrides?: RequestInit | runtime.InitOverrideFunction): Promise<runtime.ApiResponse<Array<ActionDefinitionBasic>>> {
        if (requestParameters['componentName'] == null) {
            throw new runtime.RequiredError(
                'componentName',
                'Required parameter "componentName" was null or undefined when calling getComponentActionDefinitions().'
            );
        }

        if (requestParameters['componentVersion'] == null) {
            throw new runtime.RequiredError(
                'componentVersion',
                'Required parameter "componentVersion" was null or undefined when calling getComponentActionDefinitions().'
            );
        }

        const queryParameters: any = {};

        const headerParameters: runtime.HTTPHeaders = {};

        const response = await this.request({
            path: `/component-definitions/{componentName}/versions/{componentVersion}/action-definitions`.replace(`{${"componentName"}}`, encodeURIComponent(String(requestParameters['componentName']))).replace(`{${"componentVersion"}}`, encodeURIComponent(String(requestParameters['componentVersion']))),
            method: 'GET',
            headers: headerParameters,
            query: queryParameters,
        }, initOverrides);

        return new runtime.JSONApiResponse(response, (jsonValue) => jsonValue.map(ActionDefinitionBasicFromJSON));
    }

    /**
     * Get a list of action definitions for a component.
     * Get a list of action definitions for a component
     */
    async getComponentActionDefinitions(requestParameters: GetComponentActionDefinitionsRequest, initOverrides?: RequestInit | runtime.InitOverrideFunction): Promise<Array<ActionDefinitionBasic>> {
        const response = await this.getComponentActionDefinitionsRaw(requestParameters, initOverrides);
        return await response.value();
    }

}

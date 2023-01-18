/* tslint:disable */
/* eslint-disable */
/**
 * OpenAPI definition
 * No description provided (generated by Openapi Generator https://github.com/openapitools/openapi-generator)
 *
 * The version of the OpenAPI document: v1
 * 
 *
 * NOTE: This class is auto generated by OpenAPI Generator (https://openapi-generator.tech).
 * https://openapi-generator.tech
 * Do not edit the class manually.
 */


import * as runtime from '../runtime';
import type {
  TaskDispatcherDefinitionModel,
} from '../models';
import {
    TaskDispatcherDefinitionModelFromJSON,
    TaskDispatcherDefinitionModelToJSON,
} from '../models';

/**
 * 
 */
export class TaskDispatcherDefinitionsApi extends runtime.BaseAPI {

    /**
     * Returns all task dispatcher definitions.
     */
    async getTaskDispatcherDefinitionsRaw(initOverrides?: RequestInit | runtime.InitOverrideFunction): Promise<runtime.ApiResponse<Array<TaskDispatcherDefinitionModel>>> {
        const queryParameters: any = {};

        const headerParameters: runtime.HTTPHeaders = {};

        const response = await this.request({
            path: `/definitions/task-dispatchers`,
            method: 'GET',
            headers: headerParameters,
            query: queryParameters,
        }, initOverrides);

        return new runtime.JSONApiResponse(response, (jsonValue) => jsonValue.map(TaskDispatcherDefinitionModelFromJSON));
    }

    /**
     * Returns all task dispatcher definitions.
     */
    async getTaskDispatcherDefinitions(initOverrides?: RequestInit | runtime.InitOverrideFunction): Promise<Array<TaskDispatcherDefinitionModel>> {
        const response = await this.getTaskDispatcherDefinitionsRaw(initOverrides);
        return await response.value();
    }

}

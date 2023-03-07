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
  ProjectInstanceModel,
} from '../models';
import {
    ProjectInstanceModelFromJSON,
    ProjectInstanceModelToJSON,
} from '../models';

export interface GetProjectInstancesRequest {
    projectIds?: Array<number>;
    tagIds?: Array<number>;
}

/**
 * 
 */
export class ProjectInstancesApi extends runtime.BaseAPI {

    /**
     * Get project instances.
     * Get project instances.
     */
    async getProjectInstancesRaw(requestParameters: GetProjectInstancesRequest, initOverrides?: RequestInit | runtime.InitOverrideFunction): Promise<runtime.ApiResponse<Array<ProjectInstanceModel>>> {
        const queryParameters: any = {};

        if (requestParameters.projectIds) {
            queryParameters['projectIds'] = requestParameters.projectIds;
        }

        if (requestParameters.tagIds) {
            queryParameters['tagIds'] = requestParameters.tagIds;
        }

        const headerParameters: runtime.HTTPHeaders = {};

        const response = await this.request({
            path: `/project-instances`,
            method: 'GET',
            headers: headerParameters,
            query: queryParameters,
        }, initOverrides);

        return new runtime.JSONApiResponse(response, (jsonValue) => jsonValue.map(ProjectInstanceModelFromJSON));
    }

    /**
     * Get project instances.
     * Get project instances.
     */
    async getProjectInstances(requestParameters: GetProjectInstancesRequest = {}, initOverrides?: RequestInit | runtime.InitOverrideFunction): Promise<Array<ProjectInstanceModel>> {
        const response = await this.getProjectInstancesRaw(requestParameters, initOverrides);
        return await response.value();
    }

}

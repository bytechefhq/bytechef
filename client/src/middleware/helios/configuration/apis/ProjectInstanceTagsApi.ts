/* tslint:disable */
/* eslint-disable */
/**
 * Automation Configuration API
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
  TagModel,
  UpdateTagsRequestModel,
} from '../models';
import {
    TagModelFromJSON,
    TagModelToJSON,
    UpdateTagsRequestModelFromJSON,
    UpdateTagsRequestModelToJSON,
} from '../models';

export interface UpdateProjectInstanceTagsRequest {
    id: number;
    updateTagsRequestModel: UpdateTagsRequestModel;
}

/**
 * 
 */
export class ProjectInstanceTagsApi extends runtime.BaseAPI {

    /**
     * Get project instance tags.
     * Get project instance tags
     */
    async getProjectInstanceTagsRaw(initOverrides?: RequestInit | runtime.InitOverrideFunction): Promise<runtime.ApiResponse<Array<TagModel>>> {
        const queryParameters: any = {};

        const headerParameters: runtime.HTTPHeaders = {};

        const response = await this.request({
            path: `/project-instance-tags`,
            method: 'GET',
            headers: headerParameters,
            query: queryParameters,
        }, initOverrides);

        return new runtime.JSONApiResponse(response, (jsonValue) => jsonValue.map(TagModelFromJSON));
    }

    /**
     * Get project instance tags.
     * Get project instance tags
     */
    async getProjectInstanceTags(initOverrides?: RequestInit | runtime.InitOverrideFunction): Promise<Array<TagModel>> {
        const response = await this.getProjectInstanceTagsRaw(initOverrides);
        return await response.value();
    }

    /**
     * Updates tags of an existing project instance.
     * Updates tags of an existing project instance
     */
    async updateProjectInstanceTagsRaw(requestParameters: UpdateProjectInstanceTagsRequest, initOverrides?: RequestInit | runtime.InitOverrideFunction): Promise<runtime.ApiResponse<void>> {
        if (requestParameters.id === null || requestParameters.id === undefined) {
            throw new runtime.RequiredError('id','Required parameter requestParameters.id was null or undefined when calling updateProjectInstanceTags.');
        }

        if (requestParameters.updateTagsRequestModel === null || requestParameters.updateTagsRequestModel === undefined) {
            throw new runtime.RequiredError('updateTagsRequestModel','Required parameter requestParameters.updateTagsRequestModel was null or undefined when calling updateProjectInstanceTags.');
        }

        const queryParameters: any = {};

        const headerParameters: runtime.HTTPHeaders = {};

        headerParameters['Content-Type'] = 'application/json';

        const response = await this.request({
            path: `/project-instances/{id}/project-instance-tags`.replace(`{${"id"}}`, encodeURIComponent(String(requestParameters.id))),
            method: 'PUT',
            headers: headerParameters,
            query: queryParameters,
            body: UpdateTagsRequestModelToJSON(requestParameters.updateTagsRequestModel),
        }, initOverrides);

        return new runtime.VoidApiResponse(response);
    }

    /**
     * Updates tags of an existing project instance.
     * Updates tags of an existing project instance
     */
    async updateProjectInstanceTags(requestParameters: UpdateProjectInstanceTagsRequest, initOverrides?: RequestInit | runtime.InitOverrideFunction): Promise<void> {
        await this.updateProjectInstanceTagsRaw(requestParameters, initOverrides);
    }

}

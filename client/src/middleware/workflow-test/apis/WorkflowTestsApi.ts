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

export interface TestWorkflowRequest {
    id: string;
    requestBody: { [key: string]: object; };
}

/**
 * 
 */
export class WorkflowTestsApi extends runtime.BaseAPI {

    /**
     * Create a request for testing a workflow.
     * Create a request for testing a workflow.
     */
    async testWorkflowRaw(requestParameters: TestWorkflowRequest, initOverrides?: RequestInit | runtime.InitOverrideFunction): Promise<runtime.ApiResponse<{ [key: string]: object; }>> {
        if (requestParameters.id === null || requestParameters.id === undefined) {
            throw new runtime.RequiredError('id','Required parameter requestParameters.id was null or undefined when calling testWorkflow.');
        }

        if (requestParameters.requestBody === null || requestParameters.requestBody === undefined) {
            throw new runtime.RequiredError('requestBody','Required parameter requestParameters.requestBody was null or undefined when calling testWorkflow.');
        }

        const queryParameters: any = {};

        const headerParameters: runtime.HTTPHeaders = {};

        headerParameters['Content-Type'] = 'application/json';

        const response = await this.request({
            path: `/workflow-tests/{id}`.replace(`{${"id"}}`, encodeURIComponent(String(requestParameters.id))),
            method: 'POST',
            headers: headerParameters,
            query: queryParameters,
            body: requestParameters.requestBody,
        }, initOverrides);

        return new runtime.JSONApiResponse<any>(response);
    }

    /**
     * Create a request for testing a workflow.
     * Create a request for testing a workflow.
     */
    async testWorkflow(requestParameters: TestWorkflowRequest, initOverrides?: RequestInit | runtime.InitOverrideFunction): Promise<{ [key: string]: object; }> {
        const response = await this.testWorkflowRaw(requestParameters, initOverrides);
        return await response.value();
    }

}

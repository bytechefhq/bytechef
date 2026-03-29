import * as runtime from '../runtime';
import type {ApproveForm} from '../models/ApproveForm';
import {ApproveFormFromJSON} from '../models/ApproveForm';

export interface GetApproveFormRequest {
    id: string;
}

export class ApproveFormApi extends runtime.BaseAPI {
    async getApproveFormRaw(
        requestParameters: GetApproveFormRequest,
        initOverrides?: RequestInit | runtime.InitOverrideFunction
    ): Promise<runtime.ApiResponse<ApproveForm>> {
        if (requestParameters['id'] == null) {
            throw new runtime.RequiredError(
                'id',
                'Required parameter "id" was null or undefined when calling getApproveForm().'
            );
        }

        const queryParameters: any = {};
        const headerParameters: runtime.HTTPHeaders = {};

        let urlPath = `/approve-form/{id}`;

        urlPath = urlPath.replace(`{${'id'}}`, encodeURIComponent(String(requestParameters['id'])));

        const response = await this.request(
            {
                headers: headerParameters,
                method: 'GET',
                path: urlPath,
                query: queryParameters,
            },
            initOverrides
        );

        return new runtime.JSONApiResponse(response, (jsonValue) => ApproveFormFromJSON(jsonValue));
    }

    async getApproveForm(
        requestParameters: GetApproveFormRequest,
        initOverrides?: RequestInit | runtime.InitOverrideFunction
    ): Promise<ApproveForm> {
        const response = await this.getApproveFormRaw(requestParameters, initOverrides);

        return await response.value();
    }
}

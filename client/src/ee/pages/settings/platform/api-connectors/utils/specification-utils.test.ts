import {HttpMethod} from '@/shared/middleware/graphql';
import {describe, expect, it} from 'vitest';

import {ParameterDefinitionI} from '../types/api-connector-wizard.types';
import {buildParameterSchema, parseSpecificationForWizard} from './specification-utils';

const SPECIFICATION = `
openapi: 3.0.0
info:
  title: petstore
  version: 1.0.0
servers:
  - url: https://api.example.com/v1
paths:
  /pets:
    get:
      operationId: listPets
      summary: List pets
      description: Lists all pets
      parameters:
        - name: limit
          in: query
          required: false
          description: Page size
          schema:
            type: integer
      responses:
        '200':
          description: Successful response
          content:
            application/json:
              schema:
                type: array
                items:
                  $ref: '#/components/schemas/Pet'
    post:
      operationId: createPet
      requestBody:
        required: true
        content:
          application/json:
            schema:
              $ref: '#/components/schemas/Pet'
      responses:
        '201':
          description: Created
  /pets/{petId}:
    delete:
      operationId: deletePet
      parameters:
        - name: petId
          in: path
          required: true
          schema:
            type: string
      responses:
        '204':
          description: Deleted
components:
  schemas:
    Pet:
      type: object
`;

describe('parseSpecificationForWizard', () => {
    it('parses endpoints, parameters, request bodies and responses from a specification', () => {
        const parsed = parseSpecificationForWizard(SPECIFICATION);

        expect(parsed).not.toBeNull();
        expect(parsed!.baseUrl).toBe('https://api.example.com/v1');
        expect(parsed!.endpoints).toHaveLength(3);

        const listPets = parsed!.endpoints.find((endpoint) => endpoint.operationId === 'listPets')!;

        expect(listPets.httpMethod).toBe(HttpMethod.Get);
        expect(listPets.path).toBe('/pets');
        expect(listPets.summary).toBe('List pets');
        expect(listPets.parameters).toHaveLength(1);
        expect(listPets.parameters[0]).toMatchObject({
            in: 'query',
            name: 'limit',
            required: false,
            type: 'integer',
        });
        expect(listPets.responses).toHaveLength(1);
        expect(listPets.responses[0].statusCode).toBe('200');
        expect(listPets.responses[0].contentType).toBe('application/json');
        expect(listPets.responses[0].schema).toContain('#/components/schemas/Pet');

        const createPet = parsed!.endpoints.find((endpoint) => endpoint.operationId === 'createPet')!;

        expect(createPet.requestBody).toMatchObject({
            contentType: 'application/json',
            required: true,
        });
        expect(createPet.requestBody!.schema).toContain('#/components/schemas/Pet');

        const deletePet = parsed!.endpoints.find((endpoint) => endpoint.operationId === 'deletePet')!;

        expect(deletePet.httpMethod).toBe(HttpMethod.Delete);
        expect(deletePet.parameters[0]).toMatchObject({in: 'path', name: 'petId', required: true});
    });

    it('drops unsupported methods and parameter locations instead of failing', () => {
        const parsed = parseSpecificationForWizard(`
openapi: 3.0.0
info:
  title: test
  version: 1.0.0
paths:
  /things:
    options:
      operationId: optionsThings
      responses:
        '200':
          description: OK
    get:
      operationId: listThings
      parameters:
        - name: session
          in: cookie
          schema:
            type: string
        - name: q
          in: query
          schema:
            type: string
      responses:
        '200':
          description: OK
`);

        expect(parsed).not.toBeNull();
        expect(parsed!.endpoints).toHaveLength(1);
        expect(parsed!.endpoints[0].operationId).toBe('listThings');
        expect(parsed!.endpoints[0].parameters).toHaveLength(1);
        expect(parsed!.endpoints[0].parameters[0].name).toBe('q');
    });

    it('returns null for unparseable input', () => {
        expect(parseSpecificationForWizard('{invalid: yaml: [')).toBeNull();
        expect(parseSpecificationForWizard('42')).toBeNull();
    });

    it('returns empty endpoints for a specification without paths', () => {
        const parsed = parseSpecificationForWizard('openapi: 3.0.0\ninfo:\n  title: empty\n  version: 1.0.0\n');

        expect(parsed).not.toBeNull();
        expect(parsed!.endpoints).toHaveLength(0);
    });

    it('preserves the raw parameter schema so format and enum survive', () => {
        const parsed = parseSpecificationForWizard(`
openapi: 3.0.0
info:
  title: test
  version: 1.0.0
paths:
  /things:
    get:
      operationId: listThings
      parameters:
        - name: since
          in: query
          schema:
            type: string
            format: date-time
            enum:
              - today
              - yesterday
      responses:
        '200':
          description: OK
`);

        const parameter = parsed!.endpoints[0].parameters[0];

        expect(parameter.type).toBe('string');
        expect(parameter.schema).toContain('date-time');
        expect(parameter.schema).toContain('yesterday');
    });
});

describe('buildParameterSchema', () => {
    const baseParameter: ParameterDefinitionI = {
        description: '',
        example: '',
        id: '1',
        in: 'query',
        name: 'since',
        required: false,
        schema: JSON.stringify({format: 'date-time', type: 'string'}),
        type: 'string',
    };

    it('returns the preserved schema while the type still matches', () => {
        expect(buildParameterSchema(baseParameter)).toEqual({format: 'date-time', type: 'string'});
    });

    it('falls back to a plain type schema when the parameter was retyped', () => {
        expect(buildParameterSchema({...baseParameter, type: 'integer'})).toEqual({type: 'integer'});
    });

    it('falls back to a plain type schema without a preserved schema or with a broken one', () => {
        expect(buildParameterSchema({...baseParameter, schema: undefined})).toEqual({type: 'string'});
        expect(buildParameterSchema({...baseParameter, schema: '{broken'})).toEqual({type: 'string'});
    });
});

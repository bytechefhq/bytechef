import {expect} from '@playwright/test';

import {workspaceRolesTest as test} from '../../fixtures/workspaceRoles';
import {RBAC_BASE_URL, createRoleUserApiContext, graphql, postGraphQl} from '../../utils/rbacUtils';

const AI_SKILLS = 'query AiSkills { aiSkills { id name } }';

test.describe('AI skills', () => {
    test('every member can see a skill, and only the owner and a tenant admin can change it', async ({
        adminApi,
        roleUsers,
    }) => {
        const ownerApi = await createRoleUserApiContext(RBAC_BASE_URL, roleUsers.EDITOR);
        const otherApi = await createRoleUserApiContext(RBAC_BASE_URL, roleUsers.ADMIN);

        try {
            const name = `rbac-skill-${Date.now()}`;

            const {createAiSkillFromInstructions: aiSkill} = await graphql<{
                createAiSkillFromInstructions: {id: string};
            }>(
                ownerApi,
                `
                    mutation CreateAiSkillFromInstructions($name: String!) {
                        createAiSkillFromInstructions(name: $name, instructions: "# rbac") {
                            id
                        }
                    }
                `,
                {name}
            );

            await test.step('another member sees it and is refused when changing it', async () => {
                const {aiSkills} = await graphql<{aiSkills: {id: string}[]}>(otherApi, AI_SKILLS, {});

                expect(aiSkills.map((curAiSkill) => curAiSkill.id)).toContain(aiSkill.id);

                const {aiSkill: readAiSkill} = await graphql<{aiSkill: {id: string}}>(
                    otherApi,
                    'query AiSkill($id: ID!) { aiSkill(id: $id) { id } }',
                    {id: aiSkill.id}
                );

                expect(readAiSkill.id).toBe(aiSkill.id);

                for (const query of [
                    'mutation UpdateAiSkill($id: ID!) { updateAiSkill(id: $id, name: "hijacked") { id } }',
                    'mutation DeleteAiSkill($id: ID!) { deleteAiSkill(id: $id) }',
                ]) {
                    const response = await postGraphQl(otherApi, query, {id: aiSkill.id});

                    expect(response.errors?.[0]?.extensions, JSON.stringify(response)).toMatchObject({
                        classification: 'FORBIDDEN',
                    });
                }
            });

            await test.step('the tenant admin sees it', async () => {
                const {aiSkills} = await graphql<{aiSkills: {id: string}[]}>(adminApi, AI_SKILLS, {});

                expect(aiSkills.map((curAiSkill) => curAiSkill.id)).toContain(aiSkill.id);
            });

            await test.step('the owner sees and deletes it', async () => {
                const {aiSkills} = await graphql<{aiSkills: {id: string}[]}>(ownerApi, AI_SKILLS, {});

                expect(aiSkills.map((curAiSkill) => curAiSkill.id)).toContain(aiSkill.id);

                const response = await postGraphQl(
                    ownerApi,
                    'mutation DeleteAiSkill($id: ID!) { deleteAiSkill(id: $id) }',
                    {
                        id: aiSkill.id,
                    }
                );

                expect(response.errors, JSON.stringify(response.errors)).toBeUndefined();
            });
        } finally {
            await ownerApi.dispose();
            await otherApi.dispose();
        }
    });
});

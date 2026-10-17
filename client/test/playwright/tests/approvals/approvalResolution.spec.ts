import {expect, mergeTests} from '@playwright/test';

import {loginTest, projectTest, workflowDeploymentTest} from '../../fixtures';
import {ApprovalFormPage} from '../../pages/approvalFormPage';
import {SERVER_URL} from '../../utils/apiUtils';
import {deployAndStartApprovalRun, startApprovalRun, uniqueRecipient, uniqueSuffix} from '../../utils/approvalRunUtils';
import {
    decodeRawResumeId,
    extractApprovalToken,
    getApprovalForm,
    newAnonymousPage,
    resumeJob,
    tamperTokenExpiry,
    tamperTokenPayload,
    tamperTokenSignature,
} from '../../utils/approvalUtils';
import {
    NUMBER_INPUT_FIELD_TYPE,
    TEXT_INPUT_FIELD_TYPE,
    buildApprovalWorkflow,
    buildPlainWaitWorkflow,
} from '../../utils/approvalWorkflowDefinitions';
import {countTaskExecutions, getJob, getTaskOutput, waitForJobStatus} from '../../utils/jobUtils';
import {extractLinks, recipientQuery, waitForMailpitMessage} from '../../utils/mailpitUtils';
import {startRequestCatcher} from '../../utils/requestCatcher';
import {skipUnlessMailpitIsReachable} from '../../utils/testEnvironment';

export const test = mergeTests(loginTest(), projectTest, workflowDeploymentTest);

test.describe('Request Approval resolution', () => {
    test.beforeEach(async ({request}) => {
        await skipUnlessMailpitIsReachable(request);
    });

    test('A1 one-click approve from the approval mail', async ({authenticatedPage, browser, deployer}) => {
        const recipient = uniqueRecipient('a1');
        const approvalFormPage = new ApprovalFormPage(await newAnonymousPage(browser));

        let approveLink = '';
        let jobId = '';

        await test.step('Trigger a run that suspends at approval_1', async () => {
            const approvalRun = await deployAndStartApprovalRun({
                body: {n: 'a1'},
                definition: buildApprovalWorkflow({
                    channels: ['email', 'approvalTask'],
                    formTitle: `A1 approval ${uniqueSuffix()}`,
                    label: `A1 one-click ${uniqueSuffix()}`,
                    recipient,
                }),
                deployer,
                page: authenticatedPage,
            });

            jobId = approvalRun.jobId;
        });

        await test.step('Mailpit has one approval mail with Approve and Discard links', async () => {
            const approvalMail = await waitForMailpitMessage(authenticatedPage.request, recipientQuery(recipient));

            const mailLinks = extractLinks(approvalMail.HTML);

            approveLink = mailLinks.find((mailLink) => mailLink.endsWith('?approved=true')) ?? '';

            expect(approveLink).toContain('/resume/v1.');
            expect(mailLinks.some((mailLink) => mailLink.endsWith('?approved=false'))).toBe(true);
        });

        await test.step('The Approve link renders a pre-selected Confirm view anonymously and resolves nothing', async () => {
            await approvalFormPage.goto(extractApprovalToken(approveLink), true);

            await expect(approvalFormPage.confirmApproveText).toBeVisible();

            await expect(approvalFormPage.confirmApprovalButton).toBeVisible();

            expect((await getJob(authenticatedPage, jobId)).status).toBe('STOPPED');
        });

        await test.step('Confirm approves the run', async () => {
            await approvalFormPage.confirmApprovalButton.click();

            await approvalFormPage.expectApproved();

            await waitForJobStatus(authenticatedPage, jobId, 'COMPLETED');
        });

        await test.step('approval_1 carries an empty approvedBy, never null', async () => {
            expect(await getTaskOutput(authenticatedPage, jobId, 'approval_1')).toEqual({
                approved: true,
                approvedBy: '',
                comment: '',
            });

            expect(await getTaskOutput(authenticatedPage, jobId, 'var_1')).toBe('approved=true by= comment=');
        });
    });

    test('A2 discard with a comment', async ({authenticatedPage, browser, deployer}) => {
        const approvalFormPage = new ApprovalFormPage(await newAnonymousPage(browser));

        const approvalRun = await deployAndStartApprovalRun({
            definition: buildApprovalWorkflow({
                channels: ['approvalTask'],
                formTitle: `A2 approval ${uniqueSuffix()}`,
                label: `A2 discard ${uniqueSuffix()}`,
            }),
            deployer,
            page: authenticatedPage,
        });

        await test.step('Open the Discard link, type a comment and confirm', async () => {
            await approvalFormPage.goto(approvalRun.token, false);

            await expect(approvalFormPage.confirmDiscardText).toBeVisible();

            await approvalFormPage.commentInput.fill('too pricey');

            await approvalFormPage.confirmDiscardButton.click();

            await approvalFormPage.expectDiscarded();
        });

        await test.step('The run completes with the discard decision and comment', async () => {
            await waitForJobStatus(authenticatedPage, approvalRun.jobId, 'COMPLETED');

            expect(await getTaskOutput(authenticatedPage, approvalRun.jobId, 'approval_1')).toMatchObject({
                approved: false,
                comment: 'too pricey',
            });

            expect(await getTaskOutput(authenticatedPage, approvalRun.jobId, 'var_1')).toContain('comment=too pricey');
        });
    });

    test('A3 structured form inputs are validated and typed', async ({
        authenticatedPage,
        browser,
        deployer,
        request,
    }) => {
        const formTitle = `A3 approval ${uniqueSuffix()}`;

        const definition = buildApprovalWorkflow({
            channels: ['approvalTask'],
            formTitle,
            inputs: [
                {fieldLabel: 'Reason', fieldName: 'reason', fieldType: TEXT_INPUT_FIELD_TYPE, required: true},
                {fieldLabel: 'Amount', fieldName: 'amount', fieldType: NUMBER_INPUT_FIELD_TYPE, required: true},
                {fieldLabel: 'Note', fieldName: 'note', fieldType: TEXT_INPUT_FIELD_TYPE, required: false},
            ],
            label: `A3 form ${uniqueSuffix()}`,
            nextStepValue: 'reason=${approval_1.reason} amount=${approval_1.amount} note=[${approval_1.note}]',
        });

        const approvalRun = await deployAndStartApprovalRun({definition, deployer, page: authenticatedPage});

        await test.step('The form definition is served anonymously with both inputs required', async () => {
            const formResponse = await getApprovalForm(request, approvalRun.token);

            expect(formResponse.status()).toBe(200);

            const formDefinition = JSON.stringify(await formResponse.json());

            expect(formDefinition).toContain('reason');
            expect(formDefinition).toContain('amount');
            expect(formDefinition).toContain('"required":true');
        });

        await test.step('Submitting without the required inputs is rejected', async () => {
            const missingInputsResponse = await resumeJob({body: {approved: true}, request, token: approvalRun.token});

            expect(missingInputsResponse.status()).toBe(400);
            expect(await missingInputsResponse.text()).toContain(
                "The approval form is incomplete: 'Reason' is required; 'Amount' is required."
            );
        });

        await test.step('A non-numeric or empty amount is rejected', async () => {
            const notANumberResponse = await resumeJob({
                body: {amount: 'abc', approved: true, reason: 'r'},
                request,
                token: approvalRun.token,
            });

            expect(notANumberResponse.status()).toBe(400);
            expect(await notANumberResponse.text()).toContain("'Amount' must be a number.");

            const emptyAmountResponse = await resumeJob({
                body: {amount: '', approved: true, reason: 'r'},
                request,
                token: approvalRun.token,
            });

            expect(emptyAmountResponse.status()).toBe(400);
            expect(await emptyAmountResponse.text()).toContain("'Amount' is required.");

            expect((await getJob(authenticatedPage, approvalRun.jobId)).status).toBe('STOPPED');
        });

        const anonymousPage = await newAnonymousPage(browser);
        const approvalFormPage = new ApprovalFormPage(anonymousPage);

        await test.step('The browser form blocks an empty Amount without sending a request', async () => {
            await approvalFormPage.goto(approvalRun.token);

            await expect(approvalFormPage.formTitleHeading(formTitle)).toBeVisible();

            let resumeRequestSent = false;

            anonymousPage.on('request', (sentRequest) => {
                if (sentRequest.url().includes('/job/resume/')) {
                    resumeRequestSent = true;
                }
            });

            await anonymousPage.getByLabel('Reason').fill('budget ok');

            await approvalFormPage.approveButton.click();

            await expect(approvalFormPage.fieldRequiredMessage.first()).toBeVisible();

            expect(resumeRequestSent).toBe(false);
        });

        await test.step('Valid values approve the run and keep the amount a number', async () => {
            await anonymousPage.getByLabel('Amount').fill('7');

            await approvalFormPage.approveButton.click();

            await approvalFormPage.expectApproved();

            await waitForJobStatus(authenticatedPage, approvalRun.jobId, 'COMPLETED');

            const approvalOutput = await getTaskOutput<Record<string, unknown>>(
                authenticatedPage,
                approvalRun.jobId,
                'approval_1'
            );

            expect(approvalOutput).toMatchObject({amount: 7, approved: true, reason: 'budget ok'});

            expect(await getTaskOutput(authenticatedPage, approvalRun.jobId, 'var_1')).toBe(
                'reason=budget ok amount=7 note=[]'
            );
        });

        await test.step('A discard needs no inputs', async () => {
            const secondRun = await startApprovalRun({
                deployedProject: approvalRun.deployedProject,
                deployer,
                page: authenticatedPage,
            });

            const discardResponse = await resumeJob({
                body: {approved: false, comment: 'no inputs'},
                request,
                token: secondRun.token,
            });

            expect(discardResponse.status()).toBe(204);

            await waitForJobStatus(authenticatedPage, secondRun.jobId, 'COMPLETED');

            expect(await getTaskOutput(authenticatedPage, secondRun.jobId, 'approval_1')).toMatchObject({
                amount: null,
                approved: false,
                comment: 'no inputs',
                reason: '',
            });
        });
    });

    test('A4 link scanners cannot resolve an approval', async ({authenticatedPage, browser, deployer, request}) => {
        const approvalRun = await deployAndStartApprovalRun({
            definition: buildApprovalWorkflow({
                channels: ['approvalTask'],
                formTitle: `A4 approval ${uniqueSuffix()}`,
                label: `A4 scanners ${uniqueSuffix()}`,
            }),
            deployer,
            page: authenticatedPage,
        });

        await test.step('GET and HEAD on every emailed link return the SPA page', async () => {
            for (const query of ['?approved=true', '?approved=false', '']) {
                const getResponse = await request.get(`/resume/${approvalRun.token}${query}`);

                expect(getResponse.status()).toBe(200);
                expect(getResponse.headers()['content-type']).toContain('text/html');

                const headResponse = await request.head(`/resume/${approvalRun.token}${query}`);

                expect(headResponse.status()).toBe(200);
            }

            expect((await getJob(authenticatedPage, approvalRun.jobId)).status).toBe('STOPPED');
        });

        await test.step('The one-click page waits for an explicit Confirm', async () => {
            const approvalFormPage = new ApprovalFormPage(await newAnonymousPage(browser));

            await approvalFormPage.goto(approvalRun.token, true);

            await expect(approvalFormPage.confirmApprovalButton).toBeVisible();

            await expect(approvalFormPage.approvedHeading).toBeHidden();

            expect((await getJob(authenticatedPage, approvalRun.jobId)).status).toBe('STOPPED');
        });

        await test.step('A POST still resolves the approval', async () => {
            const resumeResponse = await resumeJob({body: {approved: true}, request, token: approvalRun.token});

            expect(resumeResponse.status()).toBe(204);

            await waitForJobStatus(authenticatedPage, approvalRun.jobId, 'COMPLETED');
        });
    });

    test('A5 replayed and tampered tokens are rejected', async ({authenticatedPage, deployer, request}) => {
        const definition = buildApprovalWorkflow({
            channels: ['approvalTask'],
            formTitle: `A5 approval ${uniqueSuffix()}`,
            label: `A5 tokens ${uniqueSuffix()}`,
        });

        const resolvedRun = await deployAndStartApprovalRun({definition, deployer, page: authenticatedPage});

        await test.step('Replaying a resolved token answers 410 and the form is gone', async () => {
            expect((await resumeJob({body: {approved: true}, request, token: resolvedRun.token})).status()).toBe(204);

            await waitForJobStatus(authenticatedPage, resolvedRun.jobId, 'COMPLETED');

            expect((await resumeJob({body: {approved: true}, request, token: resolvedRun.token})).status()).toBe(410);
            expect((await resumeJob({body: {approved: false}, request, token: resolvedRun.token})).status()).toBe(410);

            const formResponse = await getApprovalForm(request, resolvedRun.token);

            expect(formResponse.status()).toBe(404);
            expect(await formResponse.text()).toContain('Approval form is no longer available.');

            expect(await getTaskOutput(authenticatedPage, resolvedRun.jobId, 'approval_1')).toMatchObject({
                approved: true,
            });
        });

        await test.step('Tampered and garbage tokens answer 400 and leave the pending run alone', async () => {
            const pendingRun = await startApprovalRun({
                deployedProject: resolvedRun.deployedProject,
                deployer,
                page: authenticatedPage,
            });

            const taskExecutionCount = await countTaskExecutions(authenticatedPage, pendingRun.jobId);

            for (const tamperedToken of [
                tamperTokenSignature(pendingRun.token),
                tamperTokenExpiry(pendingRun.token),
                tamperTokenPayload(pendingRun.token),
                'garbage-token',
            ]) {
                const tamperedResponse = await resumeJob({body: {approved: true}, request, token: tamperedToken});

                expect(tamperedResponse.status(), tamperedToken).toBe(400);
            }

            expect((await getJob(authenticatedPage, pendingRun.jobId)).status).toBe('STOPPED');
            expect(await countTaskExecutions(authenticatedPage, pendingRun.jobId)).toBe(taskExecutionCount);

            expect((await resumeJob({body: {approved: true}, request, token: pendingRun.token})).status()).toBe(204);

            await waitForJobStatus(authenticatedPage, pendingRun.jobId, 'COMPLETED');
        });
    });

    test('A6 concurrent decisions resolve exactly once', async ({authenticatedPage, deployer, request}) => {
        const approvalRun = await deployAndStartApprovalRun({
            definition: buildApprovalWorkflow({
                channels: ['approvalTask'],
                formTitle: `A6 approval ${uniqueSuffix()}`,
                label: `A6 concurrent ${uniqueSuffix()}`,
            }),
            deployer,
            page: authenticatedPage,
        });

        const decisions = [true, false, true, false, true, false, true, false];

        const resumeStatuses = await test.step('Send eight decisions in parallel', async () => {
            const resumeResponses = await Promise.all(
                decisions.map((approved) => resumeJob({body: {approved}, request, token: approvalRun.token}))
            );

            return resumeResponses.map((resumeResponse) => resumeResponse.status());
        });

        await test.step('Exactly one wins and the run continues once', async () => {
            expect(resumeStatuses.filter((resumeStatus) => resumeStatus === 204)).toHaveLength(1);
            expect(resumeStatuses.filter((resumeStatus) => resumeStatus === 410)).toHaveLength(decisions.length - 1);

            await waitForJobStatus(authenticatedPage, approvalRun.jobId, 'COMPLETED');

            const winningDecision = decisions[resumeStatuses.indexOf(204)];

            expect(await getTaskOutput(authenticatedPage, approvalRun.jobId, 'approval_1')).toMatchObject({
                approved: winningDecision,
            });

            expect(await countTaskExecutions(authenticatedPage, approvalRun.jobId, 'approval_1')).toBe(1);
            expect(await countTaskExecutions(authenticatedPage, approvalRun.jobId, 'var_1')).toBe(1);
        });
    });

    test('A22 a bare GET and a missing decision never resolve', async ({authenticatedPage, deployer, request}) => {
        const approvalRun = await deployAndStartApprovalRun({
            definition: buildApprovalWorkflow({
                channels: ['approvalTask'],
                formTitle: `A22 approval ${uniqueSuffix()}`,
                label: `A22 guards ${uniqueSuffix()}`,
            }),
            deployer,
            page: authenticatedPage,
        });

        await test.step('GET and HEAD answer 405 with Allow: POST on the server and the public URL', async () => {
            for (const resumeUrl of [
                `${SERVER_URL}/job/resume/${approvalRun.token}`,
                `/job/resume/${approvalRun.token}`,
            ]) {
                const getResponse = await request.get(resumeUrl);

                expect(getResponse.status()).toBe(405);
                expect(getResponse.headers()['allow']).toContain('POST');

                const headResponse = await request.head(resumeUrl);

                expect(headResponse.status()).toBe(405);
                expect(headResponse.headers()['allow']).toContain('POST');
            }
        });

        await test.step('A POST without an explicit boolean decision answers 400', async () => {
            for (const invalidBody of [{comment: 'no decision'}, {approved: 'yes'}, {}, '']) {
                const invalidResponse = await resumeJob({body: invalidBody, request, token: approvalRun.token});

                expect(invalidResponse.status(), JSON.stringify(invalidBody)).toBe(400);
            }

            expect((await getJob(authenticatedPage, approvalRun.jobId)).status).toBe('STOPPED');
        });

        await test.step('A valid decision still resolves it', async () => {
            expect((await resumeJob({body: {approved: true}, request, token: approvalRun.token})).status()).toBe(204);

            await waitForJobStatus(authenticatedPage, approvalRun.jobId, 'COMPLETED');
        });
    });

    test('A23 an unsigned resume id cannot resolve an approval but still resumes a plain wait', async ({
        authenticatedPage,
        deployer,
        request,
    }) => {
        const approvalRun = await deployAndStartApprovalRun({
            definition: buildApprovalWorkflow({
                channels: ['approvalTask'],
                formTitle: `A23 approval ${uniqueSuffix()}`,
                label: `A23 unsigned ${uniqueSuffix()}`,
            }),
            deployer,
            page: authenticatedPage,
        });

        await test.step('The raw resume id of an approval answers 400', async () => {
            const rawResumeId = decodeRawResumeId(approvalRun.token);

            const rawResponse = await resumeJob({body: {approved: true}, request, token: rawResumeId});

            expect(rawResponse.status()).toBe(400);

            expect((await getJob(authenticatedPage, approvalRun.jobId)).status).toBe('STOPPED');

            expect((await resumeJob({body: {approved: true}, request, token: approvalRun.token})).status()).toBe(204);
        });

        await test.step('A plain wait run still accepts its raw resume id', async () => {
            const requestCatcher = await startRequestCatcher();

            try {
                const waitProject = await deployer.deploy(
                    [buildPlainWaitWorkflow(`A23 plain wait ${uniqueSuffix()}`, `${requestCatcher.baseUrl}/wait`)],
                    {projectId: await deployer.createProject()}
                );

                const waitJob = await deployer.triggerWebhookAndWaitForJob(waitProject);

                await waitForJobStatus(authenticatedPage, waitJob.id, 'STOPPED');

                const caughtRequest = await requestCatcher.waitForRequest('/wait');

                const waitToken = extractApprovalTokenFromResumeUrl(caughtRequest.body);

                const rawResponse = await resumeJob({
                    body: {hello: 'raw'},
                    request,
                    token: decodeRawResumeId(waitToken),
                });

                expect(rawResponse.status()).toBe(204);

                await waitForJobStatus(authenticatedPage, waitJob.id, 'COMPLETED');

                expect(await getTaskOutput(authenticatedPage, waitJob.id, 'wait_1')).toEqual({
                    data: {hello: 'raw'},
                    resumed: true,
                });
            } finally {
                await requestCatcher.close();
            }
        });
    });

    test('A24 the hosted form works for an anonymous approver', async ({authenticatedPage, browser, deployer}) => {
        const formTitle = `A24 approval ${uniqueSuffix()}`;

        const approvalRun = await deployAndStartApprovalRun({
            definition: buildApprovalWorkflow({
                channels: ['approvalTask'],
                formTitle,
                inputs: [
                    {fieldLabel: 'Reason', fieldName: 'reason', fieldType: TEXT_INPUT_FIELD_TYPE, required: true},
                    {fieldLabel: 'Amount', fieldName: 'amount', fieldType: NUMBER_INPUT_FIELD_TYPE, required: true},
                ],
                label: `A24 anonymous ${uniqueSuffix()}`,
            }),
            deployer,
            page: authenticatedPage,
        });

        const anonymousPage = await newAnonymousPage(browser);
        const approvalFormPage = new ApprovalFormPage(anonymousPage);

        await test.step('The approver has no session', async () => {
            const accountResponse = await anonymousPage.request.get('/api/account');

            expect(accountResponse.status()).toBe(401);
        });

        await test.step('The form renders with its inputs, comment and both decisions', async () => {
            await approvalFormPage.goto(approvalRun.token);

            await expect(approvalFormPage.formTitleHeading(formTitle)).toBeVisible();

            await expect(anonymousPage.getByLabel('Reason')).toBeVisible();
            await expect(anonymousPage.getByLabel('Amount')).toBeVisible();
            await expect(approvalFormPage.commentInput).toBeVisible();
            await expect(approvalFormPage.approveButton).toBeVisible();
            await expect(approvalFormPage.discardButton).toBeVisible();
        });

        await test.step('Approving submits the inputs', async () => {
            await anonymousPage.getByLabel('Reason').fill('anonymous ok');

            await anonymousPage.getByLabel('Amount').fill('7');

            await approvalFormPage.commentInput.fill('anonymous browser');

            await approvalFormPage.approveButton.click();

            await approvalFormPage.expectApproved();

            await waitForJobStatus(authenticatedPage, approvalRun.jobId, 'COMPLETED');

            expect(await getTaskOutput(authenticatedPage, approvalRun.jobId, 'approval_1')).toMatchObject({
                amount: 7,
                approved: true,
                comment: 'anonymous browser',
                reason: 'anonymous ok',
            });
        });

        await test.step('Opening the link again shows that the form is gone', async () => {
            await approvalFormPage.goto(approvalRun.token);

            await expect(approvalFormPage.noLongerAvailableHeading).toBeVisible();
        });
    });
});

function extractApprovalTokenFromResumeUrl(caughtBody: string): string {
    const resumeUrlMatch = caughtBody.match(/https?:\/\/[^"\s]+\/job\/resume\/(v1\.[^"\s?]+)/);

    if (resumeUrlMatch) {
        return resumeUrlMatch[1];
    }

    return extractApprovalToken(caughtBody);
}

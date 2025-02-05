---
title: Quick Start - Star Repository
description: Learn how to connect your GitHub account to ByteChef and star a repository.
---

In this guide, you will learn how to seamlessly integrate your GitHub account with ByteChef to automate the process of starring a repository. We will walk you through creating a project in ByteChef, setting up a workflow, and running it to achieve your goal.

## Connect GitHub Account

1. Login to your GitHub account.
2. Click on your profile icon in the top right corner.
3. Select **Settings** from the dropdown menu.
4. In the left sidebar, click on **Developer settings**.
5. In the left sidebar, click on **OAuth Apps**.
6. Click on **New OAuth App**.
7. Fill in the required fields:
    - **Application name**: Enter a name for your application (e.g., `Test App`).
    - **Homepage URL**: Provide the homepage URL for your application (e.g., `https://www.bytechef.io/`).
    - **Authorization callback URL**: Specify the URL where users will be redirected after authorization (e.g., `http://127.0.0.1:5173/callback`).
8. Click **Register application**.
9. Click on **Generate a new client secret**.
10. Copy the **Client ID** and **Client Secret** for later use.

<div style="position:relative;height:0;width:100%;overflow:hidden;z-index:99999;box-sizing:border-box;padding-bottom:calc(53.02672956% + 32px)">
<iframe src="https://www.guidejar.com/embed/bhsAUb5TGIexsFuLBica?type=1&controls=on" width="100%" height="100%" style="height:100%;position:absolute;inset:0" allowfullscreen frameborder="0"></iframe>
</div>

## Star a Repository

1. Navigate to the **Projects** tab in ByteChef.
2. Click on the **Create Project** button.
3. Enter a project name. Optionally, add a description, select a category, and include relevant tags.
4. Click **Save**.
5. Click on arrow under the project name.
6. Click on the **Create Workflow** button.
7. Enter a label for your workflow and optionally provide a description.
8. Click **Save**.
9. Click on the **+** button to add component.
10. Find and select the GitHub component.
11. Choose the **Star Repository** action from the list.
12. Click on the **Connection** tab.
13. Click **Create Connection**.
14. Enter a connection name and paste the **Client ID** and **Client Secret** from GitHub.
15. Click **Next**.
16. Click **Connect**.
17. Click **Authorize**.
18. Click on **Choose Connection**.
19. Select the connection you just created.
20. Go to the **Properties** tab.
21. For **Owner**, enter the repository owner's name (e.g., `bytechefhq`).
22. For **Repository**, enter the repository name (e.g., `bytechef`).
23. Click **Run** to execute the workflow. The repository will be starred.
24. Verify by checking the **Stars** tab in your GitHub account to see the newly starred repository.

<div style="position:relative;height:0;width:100%;overflow:hidden;z-index:99999;box-sizing:border-box;padding-bottom:calc(53.02672956% + 32px)">
<iframe src="https://www.guidejar.com/embed/u1ps6awfOoKVN8gbublE?type=1&controls=on" width="100%" height="100%" style="height:100%;position:absolute;inset:0" allowfullscreen frameborder="0"></iframe>
</div>

---
title: "Workflows"
description: "Learn how to create, edit, and manage workflows effectively."
---

## Create Workflow

1. Click on the three dots next to the project name where you want to add a workflow.
2. Choose **New Workflow** from the dropdown menu.
3. Provide a label for your workflow. Optionally, add a description to explain its purpose.
4. Click **Save** to create the workflow.

<div style="position:relative;height:0;width:100%;overflow:hidden;z-index:99999;box-sizing:border-box;padding-bottom:calc(51.63511188% + 32px)">
<iframe src="https://www.guidejar.com/embed/hKXp2J07qDjlTcyz4PJp?type=1&controls=on" width="100%" height="100%" style="height:100%;position:absolute;inset:0" allowfullscreen frameborder="0"></iframe>
</div>

### Add Component

1. Click on the **+** icon to add a new component to the workflow.
2. Find and select **Math Helper** component.
3. Select the **Addition** action from the list.
4. In the **Properties** tab, enter the numbers you want to add.
5. For **First Number**, input the first value (e.g., `2`).
6. For **Second Number**, input the second value (e.g., `3`).
7. Click **Run**.
8. In the lower panel, examine the input and output for the addition action to verify the results.

<div style="position:relative;height:0;width:100%;overflow:hidden;z-index:99999;box-sizing:border-box;padding-bottom:calc(51.63511188% + 32px)">
<iframe src="https://www.guidejar.com/embed/d24dp4Xg7DtEtOBmjr5C?type=1&controls=on" width="100%" height="100%" style="height:100%;position:absolute;inset:0" allowfullscreen frameborder="0"></iframe>
</div>

### Use Data Pill

1. Click on the **+** icon to add a new component to the workflow.
2. Find and select **OpenAI** component.
3. Select the **Ask** action from the list.
4. Click on the **OpenAI** component to open its configuration panel.
5. In the **Connection** tab, click **Create Connection**.
6. Enter a connection name and paste the **API Token** from OpenAI.
7. Click **Save**.
8. Click on **Choose Connection**.
9. Select the connection you just created.
10. Go to the **Properties** tab.
11. For **Model**, choose the desired model (e.g., `gpt-4`).
12. For **Role**, select the appropriate role (e.g., `user`).
13. For **Content**, enter the prompt text, such as `Tell me something about this number:`.
14. After clicking on the content field, the Data Pill Panel will open on the left. Use the data pill to insert output from a previous component.
15. Click on **Math Helper** component in Data Pill Panel.
16. Click on **mathHelper_1**, which is the output of the addition action for the Math Helper component.
17. Output of Math Helper component is now inserted into the content field. Click **Run**.
18. In the lower panel, review the input and output for each action in the workflow. Click on **OpenAI**.
19. Verify that the result of the addition action is correctly inserted in the content field.

<div style="position:relative;height:0;width:100%;overflow:hidden;z-index:99999;box-sizing:border-box;padding-bottom:calc(51.63511188% + 32px)">
<iframe src="https://www.guidejar.com/embed/b6afQMMbdVz0ZR5s6ano?type=1&controls=on" width="100%" height="100%" style="height:100%;position:absolute;inset:0" allowfullscreen frameborder="0"></iframe>
</div>

### Edit Component

1. Click on the component you want to edit.
2. Change title and add some notes.

<div style="position:relative;height:0;width:100%;overflow:hidden;z-index:99999;box-sizing:border-box;padding-bottom:calc(51.63511188% + 32px)">
<iframe src="https://www.guidejar.com/embed/owUEJCVUqSi3T7YlqDHx?type=1&controls=on" width="100%" height="100%" style="height:100%;position:absolute;inset:0" allowfullscreen frameborder="0"></iframe>
</div>

## Edit Workflow

1. Click on the settings icon in the top right corner.
2. Choose **Edit** from the dropdown menu.
3. Change the workflow name or description as needed.
4. Click **Save** to apply your changes.

<div style="position:relative;height:0;width:100%;overflow:hidden;z-index:99999;box-sizing:border-box;padding-bottom:calc(51.63511188% + 32px)">
<iframe src="https://www.guidejar.com/embed/94Ht6KHck7Vd6sez3DaD?type=1&controls=on" width="100%" height="100%" style="height:100%;position:absolute;inset:0" allowfullscreen frameborder="0"></iframe>
</div>

## Duplicate Workflow

1. Click on the settings icon in the top right corner.
2. Choose **Duplicate** from the dropdown menu.

<div style="position:relative;height:0;width:100%;overflow:hidden;z-index:99999;box-sizing:border-box;padding-bottom:calc(51.63511188% + 32px)">
<iframe src="https://www.guidejar.com/embed/eTFQkCqlTCyjg8uHuvgP?type=1&controls=on" width="100%" height="100%" style="height:100%;position:absolute;inset:0" allowfullscreen frameborder="0"></iframe>
</div>

## Export Workflow

1. Click on the settings icon in the top right corner.
2. Choose **Export** from the dropdown menu to download the workflow JSON file.

<div style="position:relative;height:0;width:100%;overflow:hidden;z-index:99999;box-sizing:border-box;padding-bottom:calc(51.63511188% + 32px)">
<iframe src="https://www.guidejar.com/embed/V6A04t3MuaBs4lDiAn78?type=1&controls=on" width="100%" height="100%" style="height:100%;position:absolute;inset:0" allowfullscreen frameborder="0"></iframe>
</div>

## Delete Workflow

1. Click on the settings icon in the top right corner.
2. Choose **Delete** from the dropdown menu.
3. Click **Delete**.

<div style="position:relative;height:0;width:100%;overflow:hidden;z-index:99999;box-sizing:border-box;padding-bottom:calc(51.63511188% + 32px)">
<iframe src="https://www.guidejar.com/embed/6zdgSLvtHX3QAwnwQote?type=1&controls=on" width="100%" height="100%" style="height:100%;position:absolute;inset:0" allowfullscreen frameborder="0"></iframe>
</div>

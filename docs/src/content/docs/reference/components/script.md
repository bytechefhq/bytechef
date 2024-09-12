---
title: "Script"
description: "Executes user-defined code. User can write custom workflow logic in Java, JavaScript, Python, R or Ruby programming languages."
---
## Reference
<hr />

Executes user-defined code. User can write custom workflow logic in Java, JavaScript, Python, R or Ruby programming languages.


Categories: [helpers, developer-tools]


Version: 1

<hr />






## Actions


### JavaScript
Executes custom JavaScript code.

#### Properties

|      Name      |     Type     |     Control Type     |     Description     |
|:--------------:|:------------:|:--------------------:|:-------------------:|
| Input | {} | OBJECT_BUILDER  |  Initialize parameter values used in the custom code.  |
| JavaScript code | STRING | CODE_EDITOR  |  Add your JavaScript custom logic here.  |




### Python
Executes custom Python code.

#### Properties

|      Name      |     Type     |     Control Type     |     Description     |
|:--------------:|:------------:|:--------------------:|:-------------------:|
| Input | {} | OBJECT_BUILDER  |  Initialize parameter values used in the custom code.  |
| Python code | STRING | CODE_EDITOR  |  Add your Python custom logic here.  |




### Ruby
Executes custom Ruby code.

#### Properties

|      Name      |     Type     |     Control Type     |     Description     |
|:--------------:|:------------:|:--------------------:|:-------------------:|
| Input | {} | OBJECT_BUILDER  |  Initialize parameter values used in the custom code.  |
| Ruby code | STRING | CODE_EDITOR  |  Add your Ruby custom logic here.  |




<hr />

# Additional instructions
<hr />

# Calling a component inside a script

To call a component inside a script, you need to use the `context.component` object which gives you references to components and their actions. For example, to call `logger` component and its `info` action, you can use the following code:

```javascript
function perform(input, context) {
    context.component.logger.info({'text': 'Hello World!!!'})
    
	return null;
}
```

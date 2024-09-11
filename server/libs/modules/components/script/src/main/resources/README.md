# Calling a component inside a script

To call a component inside a script, you need to use the `context.component` object which gives you references to components and their actions. For example, to call `logger` component and its `info` action, you can use the following code:

```javascript
function perform(input, context) {
    context.component.logger.info({'text': 'Hello World!!!'})
    
	return null;
}
```

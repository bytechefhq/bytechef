# Calling a component inside a script

To call a component inside a script, you need to use the `context.component` object which gives you references to components and their actions. For example, to call `logger` component and its `info` action in `javascript` you can use the following code:

```javascript
function perform(input, context) {
    context.component.logger.info({'text': 'Hello World!!!'})
    
	return null;
}
```

If you want to call an action of a component which requires a connection you cna define its connection inside the Script editor:

<div style="position:relative;height:0;width:100%;overflow:hidden;z-index:99999;box-sizing:border-box;padding-bottom:calc(91.03313840% + 32px)"><iframe src="https://www.guidejar.com/embed/8d622f3f-252c-4869-9f01-108111778c71?type=1&controls=on" width="100%" height="100%" style="position:absolute;inset:0" allowfullscreen frameborder="0"></iframe></div>

You can also define multiple connections of the same component and then reference a particular connection when calling the action:

<div style="position:relative;height:0;width:100%;overflow:hidden;z-index:99999;box-sizing:border-box;padding-bottom:calc(91.03313840% + 32px)"><iframe src="https://www.guidejar.com/embed/81148ac3-e742-43a9-a852-eb1f0ab593d5?type=1&controls=on" width="100%" height="100%" style="position:absolute;inset:0" allowfullscreen frameborder="0"></iframe></div>

package com.bytechef.commons.typeconverter;

/**
 * @author Todd Fast
 */
public class TestSubclass extends TestSuperclass
		implements TypeConverter.Listener, TypeConverter.Convertible {

	public void beforeConversion(Object targetTypeKey) {
		System.out.println("--- beforeConversion("+targetTypeKey+")");
	}

	public Object afterConversion(Object targetTypeKey, Object convertedValue) {
		System.out.println("--- afterConversion("+
			targetTypeKey+","+convertedValue+")");
		return convertedValue;
	}

	public TypeConverter.Conversion getTypeConversion(final Object targetTypeKey) {
		return new TypeConverter.Conversion() {

			@Override
			public Object[] getTypeKeys() {
				return new Object[] { TestSubclass.class };
			}

			@Override
			public Object convert(Object value) {
				System.out.println("--- Converting value to type \""+key+
					"\": "+value);
				return "Converted Test value";
			}

			private Object key=targetTypeKey;
		};
	}
}

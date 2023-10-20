package com.bytechef.commons.typeconverter;

/**
 * @author Todd Fast
 */
public class StringWrapper {

	public StringWrapper(String value) {
		super();
		this.value=value;
	}

	public String toString() {
		return getWrappedString(value);
	}

	public static String getWrappedString(String value) {
		return "Wrapped value \""+value+"\"";
	}

	public static class TypeConversion implements TypeConverter.Conversion {

		public Object[] getTypeKeys() {
			return new Object[] { StringWrapper.class };
		}

		public Object convert(Object value) {
			return new StringWrapper(value.toString());
		}
	}

	private String value;
}

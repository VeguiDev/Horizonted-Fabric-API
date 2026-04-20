package net.fabricmc.loader.api.metadata;

public interface CustomValue {
	CvType getType();

	default String getAsString() {
		throw new UnsupportedOperationException();
	}

	default CvObject getAsObject() {
		throw new UnsupportedOperationException();
	}

	default CvArray getAsArray() {
		throw new UnsupportedOperationException();
	}

	enum CvType {
		STRING,
		OBJECT,
		ARRAY
	}

	interface CvObject {
		CustomValue get(String key);
	}

	interface CvArray extends Iterable<CustomValue> {
	}
}

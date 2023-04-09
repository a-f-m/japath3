package japath3.wrapper;

import java.io.IOException;
import java.util.function.Function;

import com.google.gson.GsonBuilder;
import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import com.google.gson.typeadapters.RuntimeTypeAdapterFactory;

public class GsonUtil {

	public static <T> TypeAdapter<T> createTypeAdapter(Function<T, String> toStringFunc, Function<String, T> fromStringFunc) {
		
		TypeAdapter<T> nodeAdapter = new TypeAdapter<T>() {
	
			@Override public void write(JsonWriter out, T value) throws IOException { out.value(toStringFunc.apply(value)); }
	
			@Override public T read(JsonReader in) throws IOException {
				String s = in.nextString();
				T node = fromStringFunc.apply(s);
				return node;
			}
		};
		return nodeAdapter;
	}

	public static <T> RuntimeTypeAdapterFactory<T> createRta(Class<T> clazz, java.util.List<Class> classes) {
		
		RuntimeTypeAdapterFactory<T> rta = RuntimeTypeAdapterFactory.of(clazz);
		java.util.List<Class> lll = classes;
		for (Class class1 : lll) {
			rta.registerSubtype(class1);
		}
		return rta;
	}
	
	public static GsonBuilder register(Class<?> clazz, java.util.List<Class> clazzes) {
		return new GsonBuilder().registerTypeAdapterFactory(GsonUtil.createRta(clazz, clazzes));
	}



}

package org.byteinfo.rpc;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import com.esotericsoftware.kryo.util.Pool;

import java.io.ByteArrayOutputStream;

public interface Serializer {
	Pool<Kryo> POOL = new Pool<>(true, false, 8) {
		protected Kryo create() {
			Kryo kryo = new Kryo();
			kryo.setRegistrationRequired(false);
			return kryo;
		}
	};

	static byte[] serialize(Object obj) {
		var kryo = POOL.obtain();
		var stream = new ByteArrayOutputStream(8192);
		try (var out = new Output(stream)) {
			kryo.writeObject(out, obj);
			out.flush();
			return stream.toByteArray();
		} finally {
			POOL.free(kryo);
		}
	}

	static <T> T deserialize(byte[] data, Class<T> clazz) {
		var kryo = POOL.obtain();
		try (var in = new Input(data)) {
			return kryo.readObject(in, clazz);
		} finally {
			POOL.free(kryo);
		}
	}
}

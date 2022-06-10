package japath3.processing;

import java.io.Reader;
import java.io.StringReader;
import java.util.Set;

import org.apache.commons.io.IOUtils;
import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.Source;
import org.graalvm.polyglot.Value;

import japath3.core.JapathException;

public class EngineGraal {
	
	public static boolean checkExistence = true;

	Context ctx;

	public EngineGraal() {

		try {
			ctx = Context.newBuilder("js").allowIO(true).allowAllAccess(true).option("js.load-from-url", "true").build();
		} catch (Exception e) {
			throw new JapathException(e);
		}
	}
	
	public EngineGraal eval(Reader r, String name) {

		synchronized (this) {
			try {
				String js = IOUtils.toString(r);
				if (checkExistence) {
					Set<String> existingKeys = ctx.getBindings("js").getMemberKeys();
					EngineGraal e = new EngineGraal();
					e.ctx.eval(Source.newBuilder("js", js, name).build());
					for (String string : e.ctx.getBindings("js").getMemberKeys()) {
						if (existingKeys.contains(string)) {
							throw new JapathException("javascript func '" + string + "' already defined");
						}
					}
				}

				ctx.eval(Source.newBuilder("js", js, name).build());

			} catch (Exception e) {
				throw new JapathException(e);
			}
			return this;
		}
	}
	
	public EngineGraal eval(String js, String name) {
		
		return eval(new StringReader(js), name);
	}
	
	public Value exec(String f, Object... args) {

		synchronized (this) { // TODO no shared context in graal possible
										// (do it w/ ->thread local)
			try {
				ctx.enter();
				Value func = ctx.getBindings("js").getMember(f);
				if (func == null) throw new JapathException("js func '" + f + "' does not exists");
				
				Value v = func.execute(args);
				
				return v;

			} catch (Exception e) {
				throw new JapathException(e);
			} finally {
				ctx.leave();
			}
		}
	}
	
}

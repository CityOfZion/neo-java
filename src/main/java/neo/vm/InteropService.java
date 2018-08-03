package neo.vm;

import java.util.Map;
import java.util.TreeMap;
import java.util.function.Function;

/**
 * the interoperations service.
 *
 * @author coranos
 *
 */
public class InteropService {

	/**
	 * push the alling script hash into the stack.
	 *
	 * @param engine
	 *            the engine to use.
	 * @return true of successful.
	 */
	private static boolean getCallingScriptHash(final ExecutionEngine engine) {
		engine.evaluationStack.push(AbstractStackItem.valueOf(engine.getCallingContext().getScriptHash()));
		return true;
	}

	/**
	 * push the entry script hash into the stack.
	 *
	 * @param engine
	 *            the engine to use.
	 * @return true of successful.
	 */
	private static boolean getEntryScriptHash(final ExecutionEngine engine) {
		engine.evaluationStack.push(AbstractStackItem.valueOf(engine.getEntryContext().getScriptHash()));
		return true;
	}

	/**
	 * push the executing script hash into the stack.
	 *
	 * @param engine
	 *            the engine to use.
	 * @return true of successful.
	 */
	private static boolean getExecutingScriptHash(final ExecutionEngine engine) {
		engine.evaluationStack.push(AbstractStackItem.valueOf(engine.getCurrentContext().getScriptHash()));
		return true;
	}

	/**
	 * push the script container into the stack.
	 *
	 * @param engine
	 *            the engine to use.
	 * @return true of successful.
	 */
	private static boolean getScriptContainer(final ExecutionEngine engine) {
		engine.evaluationStack.push(AbstractStackItem.fromInterface(engine.getScriptContainer()));
		return true;
	}

	/**
	 * the dictionary of services.
	 */
	private final Map<String, Function<ExecutionEngine, Boolean>> dictionary = new TreeMap<>();

	/**
	 * the constructor.
	 */
	public InteropService() {

		register("System.ExecutionEngine.GetScriptContainer", InteropService::getScriptContainer);
		register("System.ExecutionEngine.GetExecutingScriptHash", InteropService::getExecutingScriptHash);
		register("System.ExecutionEngine.GetCallingScriptHash", InteropService::getCallingScriptHash);
		register("System.ExecutionEngine.GetEntryScriptHash", InteropService::getEntryScriptHash);
	}

	/**
	 * invoke the method.
	 *
	 * @param method
	 *            the method to invokle.
	 * @param engine
	 *            the engine.
	 * @return the results of the invocation.
	 */
	public boolean invoke(final String method, final ExecutionEngine engine) {
		if (!dictionary.containsKey(method)) {
			return false;
		}
		return dictionary.get(method).apply(engine);
	}

	/**
	 * registers the function using the given method name.
	 *
	 * @param method
	 *            the method name to use.
	 * @param handler
	 *            the handler to use.
	 */
	private void register(final String method, final Function<ExecutionEngine, Boolean> handler) {
		dictionary.put(method, handler);
	}
}

// using System;
// using System.Collections.Generic;
//
// namespace Neo.VM
// {
// public class InteropService
// {
// private Dictionary<string, Func<ExecutionEngine, bool>> dictionary = new
// Dictionary<string, Func<ExecutionEngine, bool>>();
//
// public InteropService()
// {
// Register("System.ExecutionEngine.GetScriptContainer", GetScriptContainer);
// Register("System.ExecutionEngine.GetExecutingScriptHash",
// GetExecutingScriptHash);
// Register("System.ExecutionEngine.GetCallingScriptHash",
// GetCallingScriptHash);
// Register("System.ExecutionEngine.GetEntryScriptHash", GetEntryScriptHash);
// }
//
// protected void Register(string method, Func<ExecutionEngine, bool> handler)
// {
// dictionary[method] = handler;
// }
//
// internal bool Invoke(string method, ExecutionEngine engine)
// {
// if (!dictionary.ContainsKey(method)) return false;
// return dictionary[method](engine);
// }
//
// private static bool GetScriptContainer(ExecutionEngine engine)
// {
// engine.EvaluationStack.Push(StackItem.FromInterface(engine.ScriptContainer));
// return true;
// }
//
// private static bool GetExecutingScriptHash(ExecutionEngine engine)
// {
// engine.EvaluationStack.Push(engine.CurrentContext.ScriptHash);
// return true;
// }
//
// private static bool GetCallingScriptHash(ExecutionEngine engine)
// {
// engine.EvaluationStack.Push(engine.CallingContext.ScriptHash);
// return true;
// }
//
// private static bool GetEntryScriptHash(ExecutionEngine engine)
// {
// engine.EvaluationStack.Push(engine.EntryContext.ScriptHash);
// return true;
// }
// }
// }

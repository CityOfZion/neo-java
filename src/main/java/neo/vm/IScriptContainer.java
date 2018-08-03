package neo.vm;

/**
 * the script container.
 *
 * @author coranos
 *
 */
public interface IScriptContainer extends IInteropInterface {

	/**
	 * return the message.
	 *
	 * @return the message.
	 */
	byte[] getMessage();
}

// namespace Neo.VM
// {
// public interface IScriptContainer : IInteropInterface
// {
// byte[] GetMessage();
// }
// }

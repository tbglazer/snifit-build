package il.co.fibi.snifit.ant.extras.tasks;

import java.io.PrintStream;
import java.util.Vector;
import org.apache.tools.ant.BuildEvent;
import org.apache.tools.ant.BuildLogger;

public class CapturedMessages implements BuildLogger {
	private static Vector<String> v = new Vector<String>();

	private static int captureLevel = 2;

	private static boolean capturing = true;

	private static boolean tempCapturing = capturing;

	private void capture(String msg, int level) {
		if (capturing && level <= captureLevel)
			v.add(msg);
	}

	public void start() {
		v = new Vector<String>();
		capturing = true;
	}

	public void clear() {
		v = new Vector<String>();
	}

	public void stop() {
		capturing = false;
	}

	public void saveCapturing() {
		tempCapturing = capturing;
		capturing = false;
	}

	public void restoreCapturing() {
		capturing = tempCapturing;
	}

	public String[] getMessageArray() {
		int length = v.size();
		String[] messages = new String[length];
		for (int i = 0; i < length; i++)
			messages[i] = v.elementAt(i);
		return messages;
	}

	public String getLongMessage() {
		int length = v.size();
		String longMessage = "";
		for (int i = 0; i < length; i++) {
			if (i == 0) {
				longMessage = v.elementAt(i);
			} else {
				longMessage = String.valueOf(longMessage) + "\n" + (String) v.elementAt(i);
			}
		}
		return longMessage;
	}

	public String getWsadminMessages() {
		int length = v.size();
		for (int i = 0; i < length; i++) {
			String msg = v.elementAt(i);
			msg = msg.trim();
			if (msg.startsWith("[wsadmin] ")) {
				msg = msg.substring(10);
				v.setElementAt(msg, i);
			}
			if (msg.startsWith("WASX7209I: ") || msg.length() == 0) {
				v.removeElementAt(i);
				length--;
				i = -1;
			}
		}
		String longMessage = getLongMessage();
		return longMessage;
	}

	public String findMessage(String target, String context) {
		int length = v.size();
		String message = null;
		boolean contains = false;
		boolean equals = false;
		boolean startswith = false;
		boolean endswith = false;
		if (context == null) {
			contains = true;
		} else if (context.equalsIgnoreCase("equals")) {
			equals = true;
		} else if (context.equalsIgnoreCase("startswith")) {
			startswith = true;
		} else if (context.equalsIgnoreCase("endswith")) {
			endswith = true;
		} else {
			contains = true;
		}
		for (int i = 0; i < length; i++) {
			String msg = v.elementAt(i);
			if (contains)
				if (msg.indexOf(target) >= 0) {
					message = setmessage(message, msg);
				} else if (startswith) {
					if (msg.startsWith(target)) {
						message = setmessage(message, msg);
					} else if (endswith) {
						if (msg.endsWith(target)) {
							message = setmessage(message, msg);
						} else if (equals && msg.equals(target)) {
							message = setmessage(message, msg);
						}
					}
				}
		}
		return message;
	}

	private String setmessage(String originalMessage, String msg) {
		String newMessage = null;
		if (originalMessage == null) {
			newMessage = msg;
		} else {
			newMessage = String.valueOf(originalMessage) + "\n" + msg;
		}
		return newMessage;
	}

	public void buildStarted(BuildEvent event) {
		capture("BuildStarted", 3);
	}

	public void buildFinished(BuildEvent event) {
		capture("buildFinished", 3);
	}

	public void targetStarted(BuildEvent event) {
		capture("targetStarted: " + event.getTarget(), 3);
	}

	public void targetFinished(BuildEvent event) {
		capture("targetFinished: " + event.getTarget(), 3);
	}

	public void taskStarted(BuildEvent event) {
		capture("taskStarted: " + event.getTask(), 3);
	}

	public void taskFinished(BuildEvent event) {
		capture("taskFinished: " + event.getTask(), 3);
	}

	public void messageLogged(BuildEvent event) {
		String msg = "";
		StringBuffer buf = new StringBuffer();
		if (event.getTask() != null) {
			String name = event.getTask().getTaskName();
			String label = "[" + name + "] ";
			int size = 12 - label.length();
			for (int i = 0; i < size; i++)
				buf.append(" ");
			msg = label;
		}
		msg = String.valueOf(msg) + event.getMessage();
		capture(msg, event.getPriority());
	}

	public void setMessageOutputLevel(int level) {
		if (level >= 0 && level <= 4)
			captureLevel = level;
	}

	public void setOutputPrintStream(PrintStream output) {
	}

	public void setEmacsMode(boolean emacsMode) {
	}

	public void setErrorPrintStream(PrintStream err) {
	}
}

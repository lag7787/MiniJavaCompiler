package miniJava;

import java.util.List;
import java.util.ArrayList;

public class ErrorReporter {
	private List<String> _errorQueue;
	
	public ErrorReporter() {
		this._errorQueue = new ArrayList<String>();
	}
	
	public boolean hasErrors() {
		if (this._errorQueue.isEmpty()) {
			return false;
		} else {
			return true;
		}
	}
	
	public void outputErrors() {
		for (String string : _errorQueue) {
			System.out.println(string);
		}
		_errorQueue.clear();
	}
	
	public void reportError(int lineNumber, int columnNumber, String ...error) {
		StringBuilder sb = new StringBuilder();
		sb.append(String.format("line number: %d column number: %d ->\n", lineNumber, columnNumber));
		
		for(String s : error)
			sb.append("\t" + s);
		
		_errorQueue.add(sb.toString());
	}

	public void reportError(String error) {
		_errorQueue.add(error);
	}
}
package il.co.fibi.snifit.support.trace.core;

import java.util.Collection;
import java.util.Map;
import org.eclipse.osgi.service.debug.DebugTrace;

public class InternalDebugTrace implements DebugTrace {
  private final DebugTrace traceObject;
  
  public InternalDebugTrace(DebugTrace traceObj) {
    this.traceObject = traceObj;
  }
  
  public void traceObject(String option, Object attribute) {
    String message = null;
    if (attribute != null) {
      if (attribute.getClass().isArray()) {
        message = InternalTrace.convertToString("Attribute trace", (Object[])attribute);
      } else if (attribute instanceof Collection) {
        message = InternalTrace.convertToString("Attribute trace", (Collection)attribute);
      } else if (attribute instanceof Map) {
        message = InternalTrace.convertToString("Attribute trace", (Map<?, ?>)attribute);
      } else {
        message = InternalTrace.convertToString("Attribute trace", attribute);
      } 
    } else {
      message = "Attribute is null";
    } 
    this.traceObject.trace(option, message);
  }
  
  public void trace(String option, String format, Object... args) {
    String formatResult = String.format(format, args);
    this.traceObject.trace(option, formatResult);
  }
  
  public void trace(String option, String format, Throwable error, Object... args) {
    String formatResult = String.format(format, args);
    this.traceObject.trace(option, formatResult, error);
  }
  
  public void trace(String option, String message) {
    this.traceObject.trace(option, message);
  }
  
  public void trace(String option, String message, Throwable error) {
    this.traceObject.trace(option, message, error);
  }
  
  public void traceDumpStack(String option) {
    this.traceObject.traceDumpStack(option);
  }
  
  public void traceEntry(String option) {
    this.traceObject.traceEntry(option);
  }
  
  public void traceEntry(String option, Object methodArgument) {
    this.traceObject.traceEntry(option, methodArgument);
  }
  
  public void traceEntry(String option, Object[] methodArguments) {
    this.traceObject.traceEntry(option, methodArguments);
  }
  
  public void traceExit(String option) {
    this.traceObject.traceExit(option);
  }
  
  public void traceExit(String option, Object result) {
    this.traceObject.traceExit(option);
  }
}

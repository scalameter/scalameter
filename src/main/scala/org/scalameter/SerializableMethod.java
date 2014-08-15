package org.scalameter;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

//Makes the object Method Serializable, taken from http://stackoverflow.com/questions/4919205/java-serializing-methods
public class SerializableMethod implements Serializable {
    private static final long serialVersionUID = 6631604036553063682L;
    private Method method;

    public SerializableMethod(Method method)
    {
        this.method = method;
    }

    public Method getMethod()
    {
        return method;
    }
    public void invoke(Object o){
    	try {
			method.invoke(o);
		} catch (IllegalAccessException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IllegalArgumentException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (InvocationTargetException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
    }
    public Object invokeA(Object o1, Object o2){
    	Object ret = null;
    	try {
			ret = method.invoke(o1, o2);
		} catch (IllegalAccessException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IllegalArgumentException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (InvocationTargetException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
    	return ret;
    }
    private void writeObject(ObjectOutputStream out) throws IOException
    {
        out.writeObject(method.getDeclaringClass());
        out.writeUTF(method.getName());
        out.writeObject(method.getParameterTypes());
    }

    private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException
    {
        Class<?> declaringClass = (Class<?>)in.readObject();
        String methodName = in.readUTF();
        Class<?>[] parameterTypes = (Class<?>[])in.readObject();
        try
        {
            method = declaringClass.getMethod(methodName, parameterTypes);
        }
        catch (Exception e)
        {
            throw new IOException(String.format("Error occurred resolving deserialized method '%s.%s'", declaringClass.getSimpleName(), methodName), e);
        }
    }
}
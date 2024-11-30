package dev.xorcery.alchemy.script;

import org.glassfish.hk2.api.ServiceLocator;
import org.openjdk.nashorn.api.scripting.AbstractJSObject;
import org.openjdk.nashorn.api.scripting.JSObject;

public class ServicesJSObject
        extends AbstractJSObject
{
    private final ServiceLocator serviceLocator;

    public ServicesJSObject(ServiceLocator serviceLocator) {
        this.serviceLocator = serviceLocator;
    }

    @Override
    public Object getMember(String name) {
        return serviceLocator.getService(JSObject.class, name);
    }
}

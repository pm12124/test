package ext.deere.mcad.mbdviewer;

import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;

/**
 * Context class to get to the Spring Application Context from non-bean classes
 */
@Component
public class SpringContext implements ApplicationContextAware {
    private static ApplicationContext context;

    /**
     * Returns the Spring managed bean instance of the given class type if it exists.
     * Returns null otherwise.
     * @param beanClass
     * @return
     */
    public static <T extends Object> T getBean(Class<T> beanClass) {
        return context.getBean(beanClass);
    }

    /**
     * Gets the requested property value from the application.properties file
     * 
     * @param propertyName property name to get
     * @return value of the requested property
     */
    public static String getProperty(String propertyName) {
        return context.getEnvironment().getProperty(propertyName);
    }

    @Override
    public void setApplicationContext(ApplicationContext context) throws BeansException {
        SpringContext.context = context;
    }
}

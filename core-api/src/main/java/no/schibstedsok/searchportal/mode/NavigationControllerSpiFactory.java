// Copyright (2006-2007) Schibsted Søk AS
package no.schibstedsok.searchportal.mode;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.Properties;

import no.schibstedsok.searchportal.mode.command.SearchCommand;
import no.schibstedsok.searchportal.mode.config.CommandConfig.Controller;
import no.schibstedsok.searchportal.mode.config.SearchConfiguration;
import no.schibstedsok.searchportal.mode.navigation.NavigationControllerFactory;
import no.schibstedsok.searchportal.site.config.*;
import no.schibstedsok.searchportal.site.SiteContext;
import no.schibstedsok.searchportal.site.Site;


/**
 * 
 * @author magnuse
 * @version $Id: $
 */
public final class NavigationControllerSpiFactory {

    public interface Context extends SiteContext, BytecodeContext {}

    private final Context context;

    /**
     *
     * @param context
     */
    public NavigationControllerSpiFactory(final Context context) {
        this.context = context;
    }


    public NavigationControllerFactory getController(final NavigationConfig.Nav navConf){

        final String controllerName = navConf.getClass().getAnnotation(NavigationConfig.Nav.ControllerFactory.class).value();

        try{

            final SiteClassLoaderFactory.Context classContext = new SiteClassLoaderFactory.Context() {
                public BytecodeLoader newBytecodeLoader(final SiteContext site, final String name, final String jar) {
                    return context.newBytecodeLoader(site, name, jar);
                }

                public Site getSite() {
                    return context.getSite();
                }

                public Spi getSpi() {
                    return Spi.SEARCH_COMMAND_CONTROL;
                }
            };

            final SiteClassLoaderFactory loaderFactory = SiteClassLoaderFactory.valueOf(classContext);

            final Class<? extends NavigationControllerFactory> factory
                    = (Class<? extends NavigationControllerFactory>) loaderFactory.getClassLoader().loadClass(controllerName);

            final Constructor<? extends NavigationControllerFactory> constructor
                    = factory.getConstructor();

            return constructor.newInstance();

        } catch (ClassNotFoundException ex) {
            throw new IllegalArgumentException(ex);
        } catch (NoSuchMethodException ex) {
            throw new IllegalArgumentException(ex);
        } catch (InvocationTargetException ex) {
            throw new IllegalArgumentException(ex);
        } catch (InstantiationException ex) {
            throw new IllegalArgumentException(ex);
        } catch (IllegalAccessException ex) {
            throw new IllegalArgumentException(ex);
        }
    }
}
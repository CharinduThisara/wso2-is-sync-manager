package org.wso2.custom.user.operation.event.listener.internal;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.osgi.framework.BundleContext;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.*;

import org.wso2.carbon.user.core.listener.UserOperationEventListener;
import org.wso2.carbon.user.core.listener.GroupOperationEventListener;
import org.wso2.carbon.user.core.service.RealmService;
import org.wso2.custom.user.operation.event.listener.CustomUserOperationEventListener;
import org.wso2.custom.user.operation.event.listener.CustomGroupOperationEventListener;

@Component(
        name = "org.wso2.custom.user.operation.event.listener",
        immediate = true
)
public class CustomUserOperationEventListenerServiceComponent {

    private static Log log = LogFactory.getLog(CustomUserOperationEventListenerServiceComponent.class);
    private static RealmService realmService;
    CustomGroupOperationEventListener customGroupOperationEventListener;
    CustomUserOperationEventListener customUserOperationEventListener;

    @Activate
    protected void activate(ComponentContext context) {
        BundleContext bundleContext = context.getBundleContext();

        customGroupOperationEventListener = new CustomGroupOperationEventListener();
        customUserOperationEventListener = new CustomUserOperationEventListener();

        bundleContext.registerService(UserOperationEventListener.class.getName(), customUserOperationEventListener, null);
        bundleContext.registerService(GroupOperationEventListener.class.getName(), customGroupOperationEventListener, null);

        log.info("CustomUserOperationEventListener bundle activated successfully..");
        log.info("..................................................................................................");
        log.info("..................................................................................................");
        log.info("..................................................................................................");
        log.info("..................................................................................................");

    }

    @Deactivate
    protected void deactivate(ComponentContext context) {

        // Closing the Cosmos session
        customUserOperationEventListener.close();
        if (log.isDebugEnabled()) {
            log.info("CustomUserStoreManager bundle is deactivated");
        }
    }

    @Reference(
            name = "user.realmservice.default",
            service = RealmService.class,
            cardinality = ReferenceCardinality.MANDATORY,
            policy = ReferencePolicy.DYNAMIC,
            unbind = "unsetRealmService"
    )
    protected void setRealmService(RealmService realmService) {
        log.debug("Setting the Realm Service");
        CustomUserOperationEventListenerServiceComponent.realmService = realmService;
    }

    protected void unsetRealmService(RealmService realmService) {
        log.debug("UnSetting the Realm Service");
        CustomUserOperationEventListenerServiceComponent.realmService = null;
    }

    public static RealmService getRealmService() {
        return realmService;
    }
}

package com.sync.tool.internal;

import org.osgi.framework.BundleContext;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.*;
import org.wso2.carbon.user.core.service.RealmService;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import com.sync.tool.SyncTool;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


@Component(
    name = "com.sync.tool",
    immediate = true
)
public class SyncToolService {

    private static final Log log = LogFactory.getLog(SyncToolService.class);
    private static RealmService realmService;
    private static SyncTool syncTool;
    private final ExecutorService executorService = Executors.newSingleThreadExecutor();

    @Activate
    protected void activate(ComponentContext context) {
        BundleContext bundleContext = context.getBundleContext();
        syncTool = new SyncTool();
        bundleContext.registerService(SyncTool.class.getName(), syncTool, null);
        log.info("SyncTool bundle is activated");
        log.info("-------------------------------------");
        log.info("-------------------------------------");
        log.info("-------------------------------------");
        log.info("-------------------------------------");
        log.info("-------------------------------------");
        log.info("-------------------------------------");
        log.info("-------------------------------------");

        executorService.submit(() -> syncTool.read());

    }

    @Deactivate
    protected void deactivate(ComponentContext context) {
        log.info("SyncTool bundle is deactivated");
        syncTool.close();
        executorService.shutdown();
    }

    @Reference(
        name = "realm.service",
        service = RealmService.class,
        cardinality = ReferenceCardinality.MANDATORY,
        policy = ReferencePolicy.DYNAMIC,
        unbind = "unsetRealmService"
    )
    protected void setRealmService(RealmService realmService) {
        SyncToolService.realmService = realmService;
    }

    protected void unsetRealmService(RealmService realmService) {
        SyncToolService.realmService = null;
    }
    
    public static RealmService getRealmService() {
        return realmService;
    }
}

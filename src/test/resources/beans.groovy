import com.gfrison.easyweb.*
import com.gfrison.easyweb.tmpl.Template

beans {
    xmlns context: "http://www.springframework.org/schema/context"
    context.'annotation-config'()
    //context.'component-scan'('base-package': "com.gfrison")

    jsonMatcher(JsonMatcher) {
        log = log
    }
    staticResources(StaticResources) {
        container = container
        conf = container.config.static
        vertx = vertx
        log = log
        matcher = jsonMatcher
    }
    render(Template) { bean ->
        vertx = vertx
        container = container
        conf = container.config.template
        aStatic = staticResources
        log = log
        //bean.factoryMethod = 'init'
    }
    dbutil(DB) {
        eventBus = vertx.eventBus
        log = log
    }
    versioning(EnvironmentVersioning) {
        log = log
        container = container
        env = container.config.env
        mongoConf = container.config.mongo
    }
    session(Session) {
        vertx = vertx
        dbutil = dbutil
        timeout = container.config.session.timeout
        log = log
    }


}
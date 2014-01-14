vertx-easyweb
=============

Vert-X module with some facilities for Groovy web developers.

Dependency Injection with Spring
-------------------------------- 

Look at [beans.groovy](http://github.com/gfrison/vertx-easyweb/blob/master/src/test/resources/beans.groovy) as an example. This is the Grails DSL for building the application structure. Just follow the documentation on [Runtime Spring with the Beans DSL](http://grails.org/doc/latest/guide/single.html#springdsl)

Extending [SpringVerticle](http://github.com/gfrison/vertx-easyweb/blob/master/src/main/groovy/com/gfrison/easyweb/SpringVerticle.groovy) you may leverage the Spring context adding IoC feature to your application. At startup the system load the context (beans.groovy) and run the init() method.
Automatically, all beans initialized on spring context are available as properties in your SpringVerticle implementation:
<pre>
..beans.groovy
beans {
  myList(ArrayList)
}

..MySpringVerticle.groovy
def init(){
  myList.add 'item1'
}
</pre>

Static resources web server
---------------------------

Using [StaticResources](http://github.com/gfrison/vertx-easyweb/blob/master/src/main/groovy/com/gfrison/easyweb/StaticResources.groovy) for mapping all files under a specified folder, make it easy to add a static web server on your Vert-X module.

You may cache the resources in order to improve performance in production stage. The component enable [HTTP caching](http://en.wikipedia.org/wiki/HTTP_ETag) and gzip compression as well, when the client is able to handle with it.

Built-in Json parser
--------------------

I wrote [JsonMatcher](http://github.com/gfrison/vertx-easyweb/blob/master/src/main/groovy/com/gfrison/easyweb/JsonMatcher.groovy) for handling json parsing in case of json posts. If the request is 'application/json' it parses the document and inject the 'body' field in the request, then passing it to the target closure

<pre>
Http request

POST /article
Content-Type: application/json

{"title":"...", "text":"...."}


post('/article', {request->
  assert request.hasProperty('body')
  assert request.body.title == '...'
})
</pre>

Session management
------------------

Simply importing [Session](http://github.com/gfrison/vertx-easyweb/blob/master/src/main/groovy/com/gfrison/easyweb/Session.groovy) as the example:
<pre>
..bean.groovy
beans {
    session(Session) {
        vertx = vertx
        dbutil = dbutil
        timeout = container.config.session.timeout
        log = log
    }
    dbutil(DB) {
        eventBus = vertx.eventBus
        log = log
    }
    jsonMatcher(JsonMatcher) {
        log = log
    }
    
    myRouter(MyRouter){
        session = session
        matcher = jsonMatcher
    }
}

and MyRouter.groovy
class MyVerticle extends SpringVerticle {
 @Delegate
 Session session
 
 def matcher
 
 @PostConstruct
 def init(){
 
  matcher.with{
  
    post('/article',{request->
      startSession(request){session->
        assert session != null
      }
    })
    
  }
 }
}
</pre>

Session component tracks with JSESSIONID Http cookie and corresponding document in 'session' mongodb collection.
the system map the 'uid' field in the session document as 'user' document, in order to have full access of logged user when you make use of Session methods (ifSession, startSession, requireSession).


DB Versioning
-------------

Extending [EnvironmentVersioning](http://github.com/gfrison/vertx-easyweb/blob/master/src/main/groovy/com/gfrison/easyweb/EnvironmentVersioning.groovy) and implementing progressive methods according to the environment (prod, test, dev) it's possible to setup changes in the DB and store the versioning.

example:
<pre>

class MyVersion extends EnvironmentVersioning {
  def prod1 = {
    db.save....
  }
  
  def prod2 = {
    .....
  }
}
</pre>

all step regularly executed are persisted in the 'versioning' collection.
The class automatically check if new steps are inserted and proceed to execute them once.






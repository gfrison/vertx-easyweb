package com.gfrison.easyweb

import groovy.util.logging.Slf4j
import org.vertx.groovy.core.buffer.Buffer
import org.vertx.java.core.Handler
import org.vertx.java.core.MultiMap
import org.vertx.java.core.http.HttpServerRequest
import org.vertx.java.core.http.HttpServerResponse
import spock.lang.Specification

/**
 * User: gfrison
 */
@Slf4j
class JsonMatcherTest extends Specification {

    void 'test correct json parsing'() {
        setup:
        def matcher = new JsonMatcher(log: log)
        def body
        matcher.post('/url', { request ->
            body = request.body
        })

        when:
        def req = Mock(HttpServerRequest) {
            method() >> 'POST'
            path() >> '/url'
            headers() >> Mock(MultiMap) {
                get('Content-Type') >> 'application/json'
            }
            params() >> Mock(MultiMap)
            response() >> Mock(HttpServerResponse)
            bodyHandler(_ as Handler) >> { Handler handler ->
                handler.handle(new Buffer('{"name":"Giancarlo"}'))
            }
        }
        matcher.route.jRM.handle(req)

        then:
        body?.name == 'Giancarlo'
    }

    void 'no json content-type'() {
        setup:
        def matcher = new JsonMatcher(log: log)
        matcher.post('/url', { request ->
        })

        def req = Mock(HttpServerRequest) {
            method() >> 'POST'
            path() >> '/url'
            headers() >> Mock(MultiMap) {
                get('Content-Type') >> 'application/x-www-form-urlencoded'
            }
            params() >> Mock(MultiMap)
            response() >> Mock(HttpServerResponse)
            0 * bodyHandler(_ as Handler)
        }
        matcher.route.jRM.handle(req)

    }

    void 'redirect in case of http://www.<domain>'() {
        setup:
        def matcher = new JsonMatcher(log: log, conf: [removeWww: true])
        matcher.get('/', { request -> })

        def req = Mock(HttpServerRequest) {
            method() >> 'GET'
            path() >> '/'
            uri() >> '/curriculum'
            headers() >> Mock(MultiMap) {
                get('Host') >> 'www.gfrison.com'
            }
            params() >> Mock(MultiMap)
            response() >> Mock(HttpServerResponse) {
                1 * setStatusCode(301)
                1 * putHeader('Location', 'http://gfrison.com/curriculum')
                1 * end()
            }
        }
        matcher.route.jRM.handle(req)
    }

}

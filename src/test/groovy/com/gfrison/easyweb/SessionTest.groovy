package com.gfrison.easyweb

import groovy.util.logging.Slf4j
import org.vertx.groovy.core.Vertx
import org.vertx.groovy.core.eventbus.EventBus
import org.vertx.groovy.core.http.HttpServerResponse
import spock.lang.Shared
import spock.lang.Specification

/**
 * User: gfrison
 */
@Slf4j
class SessionTest extends Specification {


    @Shared
    Session session

    def setupSpec() {
        session = new Session(log: log, vertx: Vertx.newVertx())
    }

    def cleanupSpec() {

    }

    void 'get cookie from request'() {
        setup:
        def jsession = '657eabcc-d0e4-4a5c-9b57-22f8e7d4c925'
        def cookie = "fbm_368482616562345=base_domain=.gfrison.com; JSESSIONID=${jsession}; __utma=115559937.639485354.1386681179.1326681179.1386681179.1; __utmz=11e559937.1386681179.1.1.utmcsr=(direct)|utmccn=(direct)|utmcmd=(none); _ga=GA1.2.639w85354.1386681s79"
        def req = [headers: [Cookie: cookie]]

        when:
        def result = session.getSessionId(req)

        then:
        result == jsession


    }

    void 'start new session'() {
        setup:
        def response = Mock(HttpServerResponse)
        def req = [headers: [:], response: response]
        def retSession
        def eb = Mock(EventBus)
        eb.send(_, _, _) >> { collection, document, after ->
            after([body: [status: 'ok', _id: 'idtest']])
            return
        }
        session.dbutil = new DB(eventBus: eb)

        when:
        session.startSession(req, { newSession ->
            retSession = newSession
        })

        then:
        retSession._id == 'idtest'
        1 * response.putHeader('Set-Cookie', { it.indexOf('JSESSIONID=idtest') != -1 })
    }

    void 'check existing in-memory session'() {
        setup:
        def response = Mock(HttpServerResponse)
        def cookie = "fbm_368482616562345=base_domain=.gfrison.com; JSESSIONID=idtest"
        def req = [headers: [Cookie: cookie], response: response]
        def retSession
        def eb = Mock(EventBus)
        session.dbutil = new DB(eventBus: eb)

        when:
        session.startSession(req, { newSession ->
            retSession = newSession
        })

        then:
        retSession._id == 'idtest'
        0 * response.putHeader('Set-Cookie', _)

    }
}

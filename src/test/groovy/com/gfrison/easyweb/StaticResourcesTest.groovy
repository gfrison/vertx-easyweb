package com.gfrison.easyweb

import com.gfrison.easyweb.StaticResources
import org.vertx.groovy.core.http.RouteMatcher
import org.vertx.groovy.platform.Container
import org.vertx.java.core.MultiMap
import org.vertx.java.core.http.HttpServerRequest
import org.vertx.java.core.http.HttpServerResponse
import org.vertx.java.core.impl.CaseInsensitiveMultiMap
import org.vertx.java.core.logging.Logger
import spock.lang.Specification

/**
 * User: gfrison
 */
class StaticResourcesTest extends Specification {

    def html = '<html><body>body</body></html>'

    def randomText = { int n ->
        def alphabet = ('a'..'z').join()
        new Random().with {
            (1..n).collect { alphabet[nextInt(alphabet.length())] }.join()
        }
    }
    CharSequence folder
    StaticResources staticResources

    def setup() {
        staticResources = new StaticResources()
        given:
        staticResources.log = Mock(Logger)
        staticResources.matcher = new RouteMatcher()
        staticResources.container = Mock(Container)
        folder = randomText(5)
        def tmpFolder = new File(System.getProperty('java.io.tmpdir') + '/' + folder)
        tmpFolder.mkdir()
        def index = new File(tmpFolder.path + '/index.html')
        index.write(html)


        staticResources.container.env >> ['STATIC_FOLDER': System.getProperty('java.io.tmpdir')]
        staticResources.conf = [cache: false, folder: System.getProperty('java.io.tmpdir')]

    }

    def cleanup() {
        def tmpFolder = new File(System.getProperty('java.io.tmpdir') + '/' + folder)
        tmpFolder.deleteDir()

    }

    void 'check init'() {

        when: 'init'
        staticResources.init()

        then: 'files are loaded'
        staticResources.files.size() > 0

        when: 'invoked'
        def handler = staticResources.matcher.asClosure()
        def req = Mock(HttpServerRequest) {
            response() >> Mock(HttpServerResponse) {
                1 * setStatusCode(404)
                1 * end()

            }
        }
        handler.jRM.noMatchHandler.handle(req)

        then: 'wrong method (not GET)'

        when: 'invoke root directory no gzip ecoding'
        req = Mock(HttpServerRequest) {
            method() >> 'GET'
            path() >> '/' + folder + '/'
            headers() >> new CaseInsensitiveMultiMap()
            response() >> Mock(HttpServerResponse) {
                1 * putHeader('Content-Type', 'text/html')
                1 * write(_)
                1 * end()

            }
        }
        handler.jRM.noMatchHandler.handle(req)

        then: 'correct index.html'

        when: 'invoke root directory gzip encoding'
        req = Mock(HttpServerRequest) {
            method() >> 'GET'
            path() >> '/' + folder + '/'
            headers() >> Mock(MultiMap) {
                get('accept-encoding') >> 'gzip'
            }
            response() >> Mock(HttpServerResponse) {
                1 * putHeader('Content-Encoding', 'gzip')
                1 * write(_)
                1 * end()

            }
        }
        handler.jRM.noMatchHandler.handle(req)

        then: 'correct gzip index.html'

        when: 'invoke file name'
        req = Mock(HttpServerRequest) {
            method() >> 'GET'
            path() >> '/' + folder + '/index.html'
            headers() >> Mock(MultiMap) {
                get('accept-encoding') >> null
            }
            response() >> Mock(HttpServerResponse) {
                1 * putHeader('Content-Type', 'text/html')
                1 * putHeader('Content-Length', '' + html.length())
                1 * write({ it.toString() == html })
                1 * end()

            }
        }
        handler.jRM.noMatchHandler.handle(req)

        then: 'correct gzip index.html'

    }
}

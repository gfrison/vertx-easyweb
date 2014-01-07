package com.gfrison.easyweb

import de.flapdoodle.embed.mongo.MongodStarter
import de.flapdoodle.embed.mongo.config.IMongodConfig
import de.flapdoodle.embed.mongo.config.MongodConfigBuilder
import de.flapdoodle.embed.mongo.config.Net
import de.flapdoodle.embed.mongo.distribution.Version
import de.flapdoodle.embed.process.runtime.Network
import groovy.util.logging.Slf4j
import spock.lang.Specification

/**
 * User: gfrison
 */
@Slf4j
class EnvironmentVersioningTest extends Specification {

    def mongod
    EnvironmentVersioning enVersioning
    def status = []

    def setup() {
        MongodStarter runtime = MongodStarter.getDefaultInstance();
        IMongodConfig mongodConfig = new MongodConfigBuilder()
                .version(Version.Main.PRODUCTION)
                .net(new Net(12345, Network.localhostIsIPv6()))
                .build();
        mongod = runtime.prepare(mongodConfig).start();

    }

    def cleanup() {
        if (mongod) {
            mongod.stop();
        }
    }

    void 'test 1'() {
        when: 'prova'
        enVersioning = new Test1Env(log: log)
        enVersioning.env = 'test'
        enVersioning.mongoConf = [host: 'localhost', port: 12345, db_name: 'test']
        enVersioning.updateVersion()
        then: status == ['test1']

        when: 'upgrading environment'
        enVersioning = new Test2Env(log: log)
        enVersioning.env = 'test'
        enVersioning.mongoConf = [host: 'localhost', port: 12345, db_name: 'test']
        enVersioning.updateVersion()
        then: status == ['test1', 'test2']

    }

    class Test1Env extends EnvironmentVersioning {
        def test1 = {
            status << 'test1'
        }
    }

    class Test2Env extends EnvironmentVersioning {
        def test1 = {
            status << 'wrong'
        }
        def test2 = {
            status << 'test2'
        }
    }
}

package com.gfrison.easyweb

import com.gmongo.GMongo
import com.mongodb.ServerAddress

import javax.annotation.PostConstruct

/**
 * Extending this class and implementing progressive methods according to the environment (prod, test, dev)
 * it 's possible to setup changes in the DB and store the versioning.
 * ex:
 * // add pre-compiled items in the DB
 * class MyVersion extends EnvironmentVersioning {*
 * def prod1 = {  db.save....
 *
 * all step regularly executed are persisted in the 'versioning' collection.
 * The class automatically check if new steps are inserted and proceed to execute them once.
 *
 * User: gfrison
 */
class EnvironmentVersioning {
    def log
    def db
    def container
    def env
    def mongoConf


    @PostConstruct
    def updateVersion() {
        def mongo
        if (mongoConf.host) {
            mongo = new GMongo(new ServerAddress(mongoConf.host, mongoConf.port))
        } else {
            mongo = new GMongo()
        }
        assert mongoConf.db_name != null: 'set db_name property on conf.json'
        db = mongo.getDB(mongoConf.db_name)
        if (mongoConf.username) {
            db.authenticate(mongoConf.username, mongoConf.password?.toCharArray())
        }
        def that = this
        def versioning = db.versioning.findOne([_id: 1])

        def version = versioning ? versioning.version : 0
        def closurePropNames = properties.findResults { name, value ->
            (value instanceof Closure && name.startsWith(env)) ? name.substring(env.length()) : null
        }.sort()
        closurePropNames.each {
            if (it > version) {
                log.info("upgrading db version to:${it}")
                that."${env}${it}"()
                db.versioning.update([_id: 1], [_id: 1, version: it], true)

            }
        }


    }

}

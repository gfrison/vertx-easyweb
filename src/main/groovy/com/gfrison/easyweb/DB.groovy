package com.gfrison.easyweb
/**
 * User: gfrison
 */
public class DB implements IUtil {
    def eventBus
    def log

    def findAll = { collection, matcher, fields, after ->
        def map = ['action': 'find', 'matcher': matcher, 'collection': collection]
        if (fields) {
            map['keys'] = fields
        }
        eventBus.send('vertx.mongopersistor', map) { res ->
            if (res.body.status == 'ok') {
                after res.body.results
            } else {
                after(false)
            }
        }
    }

    def findAllById = { collection, ids, fields, after ->
        if (!ids) {
            after([])
            return
        }
        def map = ['action': 'find', 'matcher': ['_id': ['$in': ids]], 'collection': collection]
        if (fields) {
            map['keys'] = fields
        }
        eventBus.send('vertx.mongopersistor', map) { res ->
            if (res.body.status == 'ok') {
                after res.body.results
            } else {
                after(false)
            }
        }
    }

    def find1 = { collection, matcher, after ->
        eventBus.send('vertx.mongopersistor', ['action': 'findone', 'collection': collection, 'matcher': matcher]) { res ->
            if (res.body.status == 'ok') {
                after res.body.result
            } else {
                after(false)
            }
        }
    }

    def findById = { collection, id, after ->
        find1(collection, ['_id': id], after)
    }

    def saveBy = {
        field, obj, collection, after ->
            find1(collection, ["${field}": obj."${field}"]) { dbobj ->
                if (!dbobj) {
                    log.info "insert new ${collection}:${obj}"
                    save(collection, obj) { id ->
                        obj._id = id
                        if (after) {
                            after(obj)
                        }
                    }
                    return
                }
                dbobj = dbobj + obj
                log.info "update ${collection} with ${field}:" + obj."${field}" + ", obj:${dbobj}"
                update(collection, ['_id': obj._id], dbobj)
                if (after) {
                    after(dbobj)
                }
            }
    }
    def saveUserBy = { field, user, session ->
        if (session.user) {
            log.info('update user:' + user)
            if (session.user && user.email && session.user.email != user.email) {
                log.info("user è già registrato:${session.user.email} aggiungo moreEmails ${user.email}")
                update('user', ['_id': session.user._id], ['$addToSet': ['moreEmails': user.email]])
                user.remove('email')
            }
            user.remove('_id')
            update('user', ['_id': session.user._id], ['$set': user])
            session.user = session.user + user
        } else {
            find1('user', ["${field}": user."${field}"]) { dbuser ->
                if (!dbuser) {
                    log.info('new user:' + user)
                    save('user', user) { id ->
                        user._id = id
                        session.user = user
                        update('session', ['_id': session._id], ['$set': ['uid': id]])
                    }
                    return
                }
                update('session', ['_id': session._id], ['$set': ['uid': dbuser._id]])
                log.info('update existant user:' + dbuser + ', new user:' + user)
                dbuser = dbuser + user
                update('user', ['_id': dbuser._id], dbuser)
                session.user = dbuser
            }
        }
    }

    def save = { Object... args ->
        def (collection, document, after) = args
        eventBus.send('vertx.mongopersistor', ['action': 'save', 'collection': collection, 'document': document]) { res ->
            if (res.body.status == 'ok') {
                if (after) {
                    after res.body._id
                }
            } else {
                if (after) {
                    after(false)
                }
            }
        }
    }

    def update = { collection, criteria, objNew, upsert = false, multi = false ->
        eventBus.send('vertx.mongopersistor', ['action': 'update', 'collection': collection, 'criteria': criteria, 'objNew': objNew, 'upsert': upsert, 'multi': multi])
    }

    def remove = { collection, id ->
        eventBus.send('vertx.mongopersistor', ['action': 'delete', 'collection': collection, 'matcher': ['_id': id]])
    }

    def removeBy = { collection, criteria ->
        eventBus.send('vertx.mongopersistor', ['action': 'delete', 'collection': collection, 'matcher': criteria])

    }
}
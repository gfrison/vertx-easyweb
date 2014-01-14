package com.gfrison.easyweb

import java.util.concurrent.ConcurrentHashMap

/**
 * Map implementation with TTL: the items are removed after elapsed time
 * User: gfrison
 */
class TimerMap {

    def log

    @Delegate
    Map<String, Map<String, Object>> map = new ConcurrentHashMap<String, Map<String, Object>>()

    def timeout
    def vertx

    public Object get(String key) {
        def obj = map.get(key)
        if (obj == null)
            return null

        def tid = obj?.timeid
        if (tid)
            vertx.cancelTimer(tid)
        obj.timeid = vertx.setTimer(timeout) {
            log.debug('get session timeout:' + key)
            remove(key)
        }
        return obj
    }

    public Object put(Object key, Object value) {
        map.put(key, value)
        value.timeid = vertx.setTimer(timeout) {
            log.debug('put session timeout:' + key)
            remove(key)
        }

    }
}


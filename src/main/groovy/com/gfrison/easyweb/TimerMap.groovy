package com.gfrison.easyweb

import java.util.concurrent.ConcurrentHashMap

/**
 * User: gfrison
 */
class TimerMap {

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
            println('get session timeout:' + key)
            remove(key)
        }
        return obj
    }

    public Object put(Object key, Object value) {
        map.put(key, value)
        value.timeid = vertx.setTimer(timeout) {
            println('put session timeout:' + key)
            remove(key)
        }

    }
}


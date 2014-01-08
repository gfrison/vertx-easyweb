package com.gfrison.easyweb
/**
 * User: gfrison
 */
class TestVerticle extends SpringVerticle {

    @Override
    void init() {
        assert session != null: 'spring context do not inject references properly'

    }
}

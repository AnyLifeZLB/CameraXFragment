package com.zenglb.cameraxfragment

import android.app.Application

/**
 * 需要有一个更简单的访问application context的方式
 *
 * @author zlb
 */
 class MyApplication : Application(){

    /**
     * 伴生对象
     */
    companion object{
        private var instance:Application?=null
        fun instance()= instance!!

        val languageName: String? = null

        fun getStr():String{
            return "hello world"
        }
    }

    /**
     * 创建
     */
    override fun onCreate() {
        super.onCreate()
        instance = this


        var b: String? = "abc" // 可以设置为空
        b = null // ok
        print(b)

        val l = b?.length // 错误：变量“b”可能为空


    }

}
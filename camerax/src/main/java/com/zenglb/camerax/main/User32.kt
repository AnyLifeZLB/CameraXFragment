//package com.zenglb.camerax.main
//
//class User32 private constructor(builder: UserBuilder) {
//    val firstName // 必传参数
//            : String
//    val age // 可选参数
//            : Int
//
//    class UserBuilder(private val firstName: String) {
//        private var age = 0
//        fun age(age: Int): UserBuilder {
//            this.age = age
//            return this
//        }
//
//        fun build(): User32 {
//            return User32(this)
//        }
//    }
//
//    init {
//        firstName = builder.firstName
//        age = builder.age
//    }
//}
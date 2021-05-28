package com.zenglb.camerax.main;

public class User {
    private final String firstName;     // 必传参数
    private final int age;              // 可选参数


    private User(UserBuilder builder) {
        this.firstName = builder.firstName;
        this.age = builder.age;
    }

    public String getFirstName() {
        return firstName;
    }

    public int getAge() {
        return age;
    }


    public static class UserBuilder {
        private final String firstName;
        private int age;

        public UserBuilder(String firstName) {
            this.firstName = firstName;
        }

        public UserBuilder age(int age) {
            this.age = age;
            return this;
        }


        public User build() {
            return new User(this);
        }
    }
}


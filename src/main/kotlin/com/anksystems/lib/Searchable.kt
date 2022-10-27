package com.anksystems.lib

interface Searchable<T> {
    fun compare(e:T): Boolean
}
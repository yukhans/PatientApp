package com.example.doctors

class Selection{
    private var choice: String = ""

    fun changeChoice(newChoice:String){
        choice = newChoice
    }

    fun getChoice(): String{
        return choice
    }
}
package com.coinlet

object Validator {
    fun validateCheckbox(isChecked : Boolean): Boolean{
        return isChecked
    }
    fun isPhoneValid(phoneNumber : String): Boolean{
        val regex = Regex("^\\+?[0-9]{9,20}$")
        return regex.matches(phoneNumber)
    }
    fun isEmailValid(email :String): Boolean{
        val regex = Regex("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\$")
        return regex.matches(email)
    }
    fun isFirstNameValid(firstName : String): Boolean{
        val regex = Regex("^[A-ZŁŚĆŹŻ][a-ząćęłńóśżź]+\$")
        return regex.matches(firstName)
    }
    fun isSecondNameValid(secondName : String): Boolean{
        if (secondName.isBlank()) {
            return true
        }
        val regex = Regex("^[A-ZŁŚĆŹŻ][a-ząćęłńóśżź]+\$")
        return regex.matches(secondName)
    }
    fun isLastNameValid(lastName : String): Boolean{
        val regex = Regex("^[A-ZĄĆĘŁŃÓŚŹŻ][a-ząćęłńóśżź]+(-[A-ZĄĆĘŁŃÓŚŹŻ][a-ząćęłńóśżź]+)?$")
        return regex.matches(lastName)
    }
    fun isBirthDateValid(birthDate : String): Boolean{
        val regex = Regex("^(0[1-9]|[12][0-9]|3[01])\\.(0[1-9]|1[0-2])\\.(19|20)\\d{2}\$")
        return regex.matches(birthDate)
    }
    fun isPeselValid(pesel : String): Boolean{
        val regex = Regex("^[0-9]{11}$")
        return regex.matches(pesel)
    }
    fun isCountryValid(country : String):Boolean{
        val regex = Regex("^[A-ZĄĆĘŁŃÓŚŹŻ][a-ząćęłńóśżź]+(?: [A-ZĄĆĘŁŃÓŚŹŻ][a-ząćęłńóśżź]+)*\$")
        return regex.matches(country)
    }
    fun isNumberHouseValid(houseNumber : String): Boolean{
        val regex = Regex("^[0-9]{1,4}[A-Za-z]?(\\/[0-9]{1,3})?\$")
        return regex.matches(houseNumber)
    }
    fun isStreetValid(street : String): Boolean{
        val regex = Regex("^[A-Za-zĄĆĘŁŃÓŚŹŻąćęłńóśżź0-9 .-]{2,50}$")
        return regex.matches(street)
    }
    fun isPasswordValid(password : String): Boolean{
        val regex = Regex("^(?=.*[A-Z])(?=.*[0-9]).{8,}\$")
        return regex.matches(password)
    }
    fun doPasswordsMatch(password: String, confirmPassword: String): Boolean {
        return password == confirmPassword
    }
}
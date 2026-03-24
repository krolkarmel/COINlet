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
    fun isCityValid(city: String): Boolean {
        val regex = Regex("^[A-ZĄĆĘŁŃÓŚŹŻ][a-ząćęłńóśżź]+(?:[- ][A-ZĄĆĘŁŃÓŚŹŻ][a-ząćęłńóśżź]+)*$")
        return regex.matches(city.trim())
    }

    fun isCountryValid(country: String): Boolean {
        val regex = Regex("^[A-ZĄĆĘŁŃÓŚŹŻ][a-ząćęłńóśżź]+(?: [A-ZĄĆĘŁŃÓŚŹŻ][a-ząćęłńóśżź]+)*$")
        return regex.matches(country.trim())
    }

    fun isStreetValid(street: String): Boolean {
        val regex = Regex("^[A-ZĄĆĘŁŃÓŚŹŻ][a-ząćęłńóśżź]+(?: [A-ZĄĆĘŁŃÓŚŹŻ][a-ząćęłńóśżź]+)*$")
        return regex.matches(street.trim())
    }

    fun isNumberHouseValid(houseNumber: String): Boolean {
        val number = houseNumber.trim().toIntOrNull() ?: return false
        return number in 1..9999
    }
    fun isPasswordValid(password : String): Boolean{
        val regex = Regex("^(?=.*[A-Z])(?=.*[0-9]).{8,}\$")
        return regex.matches(password)
    }
    fun doPasswordsMatch(password: String, confirmPassword: String): Boolean {
        return password == confirmPassword
    }
    fun isPinGood(pin: String): Boolean{
        val regex = Regex("^\\d{4}\$")
        return regex.matches(pin)
    }
}
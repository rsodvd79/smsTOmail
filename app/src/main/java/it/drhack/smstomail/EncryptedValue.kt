package it.drhack.smstomail

/**
 * Classe wrapper per rappresentare un valore non criptato
 * Questa classe serve a disambiguare la conversione in Room
 */
data class EncryptedValue(val value: String)

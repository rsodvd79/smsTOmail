package it.drhack.smstomail

/**
 * Classe che si occupa di verificare se un SMS corrisponde ai filtri configurati
 */
class SmsFilterProcessor(private val filters: List<Filter>) {

    /**
     * Verifica se un SMS deve essere elaborato in base ai filtri configurati.
     * Se non ci sono filtri, tutti gli SMS vengono elaborati.
     *
     * @param sender Il numero del mittente dell'SMS
     * @param message Il contenuto dell'SMS
     * @return true se l'SMS deve essere elaborato, false altrimenti
     */
    fun shouldProcessSms(sender: String, message: String): Boolean {
        if (filters.isEmpty()) {
            // Se non ci sono filtri, elabora tutti gli SMS
            return true
        }

        // Verifica se almeno un filtro corrisponde
        return filters.any { filter ->
            val senderMatch = when {
                // Se il filtro ha un asterisco come mittente, corrisponde a qualsiasi mittente
                filter.sender == "*" -> true
                // Se il filtro non ha un mittente specificato, è considerato corrispondente
                filter.sender.isBlank() -> true
                // Altrimenti, verifica se il mittente contiene il testo del filtro
                else -> sender.contains(filter.sender, ignoreCase = true)
            }

            val messageMatch = when {
                // Se il filtro ha un asterisco come parola chiave, corrisponde a qualsiasi messaggio
                filter.keyword == "*" -> true
                // Se il filtro non ha una parola chiave specificata, è considerato corrispondente
                filter.keyword.isBlank() -> true
                // Altrimenti, verifica se il messaggio contiene la parola chiave del filtro
                else -> message.contains(filter.keyword, ignoreCase = true)
            }

            when (filter.filterType) {
                FilterType.INCLUDE -> senderMatch && messageMatch
                FilterType.EXCLUDE -> !(senderMatch && messageMatch)
            }
        }
    }
}

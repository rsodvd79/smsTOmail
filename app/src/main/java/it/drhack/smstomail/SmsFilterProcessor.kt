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
            return true
        }

        val includeFilters = filters.filter { it.filterType == FilterType.INCLUDE }
        val excludeFilters = filters.filter { it.filterType == FilterType.EXCLUDE }

        // Un filtro matcha se mittente E keyword corrispondono (blank = qualsiasi valore)
        fun matches(filter: Filter): Boolean {
            val senderMatch = filter.sender.isBlank() || sender.contains(filter.sender, ignoreCase = true)
            val messageMatch = filter.keyword.isBlank() || message.contains(filter.keyword, ignoreCase = true)
            return senderMatch && messageMatch
        }

        // Se l'SMS matcha un filtro EXCLUDE, non va inoltrato (ha precedenza)
        if (excludeFilters.any { matches(it) }) return false

        // Se esistono filtri INCLUDE, almeno uno deve corrispondere
        if (includeFilters.isNotEmpty()) return includeFilters.any { matches(it) }

        // Solo filtri EXCLUDE presenti e nessuno ha fatto match: inoltra
        return true
    }
}

package com.archimedeprojects.arihna.feature.settings

import com.archimedeprojects.arihna.core.location.model.LocationFailure
import com.archimedeprojects.arihna.core.location.model.LocationFreshness
import com.archimedeprojects.arihna.core.location.model.LocationResolutionState
import com.archimedeprojects.arihna.core.location.model.LocationSource
import com.archimedeprojects.arihna.core.location.model.SelectedLocation

data class LocationStatusPresentation(
    val title: String,
    val message: String,
    val locationName: String? = null,
    val zoneId: String? = null,
    val freshness: String? = null,
    val showAppSettingsAction: Boolean = false,
    val showLocationSettingsAction: Boolean = false,
)

fun LocationModeUi.label(): String = when (this) {
    LocationModeUi.Unconfigured -> "Non configurata"
    LocationModeUi.Device -> "Device"
    LocationModeUi.Manual -> "Manuale"
}

fun LocationResolutionState.toPresentation(): LocationStatusPresentation = when (this) {
    LocationResolutionState.Unconfigured -> LocationStatusPresentation(
        title = "Posizione non configurata",
        message = "Scegli la posizione del dispositivo oppure cerca una città manualmente.",
    )

    LocationResolutionState.Resolving -> LocationStatusPresentation(
        title = "Risoluzione in corso",
        message = "Arihna sta determinando la posizione da usare per i calcoli.",
    )

    is LocationResolutionState.Ready -> {
        val isDevice = location.source is LocationSource.Device
        LocationStatusPresentation(
            title = if (isDevice) "Posizione dispositivo pronta" else "Città manuale attiva",
            message = if (isDevice) {
                "La posizione approssimativa del dispositivo è pronta per il calcolo degli orari."
            } else {
                "Arihna userà questa città e il suo fuso orario per i calcoli."
            },
            locationName = location.displayName,
            zoneId = location.zoneId.id,
            freshness = when (freshness) {
                LocationFreshness.FRESH -> "FRESH"
                LocationFreshness.CACHED -> "CACHED"
                null -> null
            },
        )
    }

    is LocationResolutionState.PermissionDenied -> LocationStatusPresentation(
        title = "Permesso posizione non concesso",
        message = appendCached(
            if (canRequestAgain) {
                "Puoi riprovare con ‘Usa posizione attuale’ oppure scegliere una città manualmente."
            } else {
                "Il permesso è disattivato per Arihna. Puoi abilitarlo dalle impostazioni dell’app oppure usare una città manuale."
            },
            cachedLocation,
        ),
        locationName = cachedLocation?.displayName,
        zoneId = cachedLocation?.zoneId?.id,
        freshness = cachedLocation?.let { "CACHED" },
        showAppSettingsAction = !canRequestAgain,
    )

    is LocationResolutionState.LocationServicesDisabled -> LocationStatusPresentation(
        title = "Servizi di localizzazione disattivati",
        message = appendCached(
            "Attiva la Posizione nelle impostazioni Android per ottenere un nuovo fix, oppure scegli una città manuale.",
            cachedLocation,
        ),
        locationName = cachedLocation?.displayName,
        zoneId = cachedLocation?.zoneId?.id,
        freshness = cachedLocation?.let { "CACHED" },
        showLocationSettingsAction = true,
    )

    is LocationResolutionState.Unavailable -> LocationStatusPresentation(
        title = failureTitle(reason),
        message = appendCached(failureMessage(reason), cachedLocation),
        locationName = cachedLocation?.displayName,
        zoneId = cachedLocation?.zoneId?.id,
        freshness = cachedLocation?.let { "CACHED" },
    )
}

private fun failureTitle(reason: LocationFailure): String = when (reason) {
    LocationFailure.TIMEOUT -> "Posizione non ricevuta"
    LocationFailure.NO_PROVIDER -> "Posizione non disponibile"
    LocationFailure.INVALID_FIX -> "Posizione non valida"
    LocationFailure.CITY_NOT_FOUND -> "Città non trovata"
    LocationFailure.CITY_DATASET_UNAVAILABLE -> "Archivio città non disponibile"
    LocationFailure.UNSUPPORTED_TIME_ZONE -> "Fuso orario non supportato"
    LocationFailure.PERSISTENCE_ERROR -> "Impossibile salvare la posizione"
}

private fun failureMessage(reason: LocationFailure): String = when (reason) {
    LocationFailure.TIMEOUT ->
        "Nessuna posizione è arrivata entro 20 secondi. Puoi riprovare o scegliere una città manuale."
    LocationFailure.NO_PROVIDER ->
        "Android non ha reso disponibile un provider di posizione utilizzabile. Puoi riprovare o scegliere una città manuale."
    LocationFailure.INVALID_FIX ->
        "Il dispositivo ha restituito una posizione non valida; Arihna non la userà."
    LocationFailure.CITY_NOT_FOUND ->
        "La città selezionata non è più disponibile nell’archivio locale."
    LocationFailure.CITY_DATASET_UNAVAILABLE ->
        "L’archivio locale delle città non è disponibile in questo momento."
    LocationFailure.UNSUPPORTED_TIME_ZONE ->
        "Questa città usa un fuso orario che questa versione di Android non può risolvere in modo affidabile. Scegli un’altra città."
    LocationFailure.PERSISTENCE_ERROR ->
        "Arihna non è riuscita a salvare la scelta della posizione. Riprova."
}

private fun appendCached(base: String, cachedLocation: SelectedLocation?): String =
    if (cachedLocation == null) {
        base
    } else {
        "$base Ultima posizione reale salvata: ${cachedLocation.displayName}; viene mostrata come CACHED, non come posizione corrente."
    }

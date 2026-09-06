package com.archimedeprojects.arihna.feature.settings

import com.archimedeprojects.arihna.core.location.model.LocationFailure
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
    LocationModeUi.Device -> "Posizione attuale"
    LocationModeUi.Manual -> "Città scelta"
}

fun LocationResolutionState.toPresentation(): LocationStatusPresentation = when (this) {
    LocationResolutionState.Unconfigured -> LocationStatusPresentation(
        title = "Posizione non configurata",
        message = "Usa la posizione attuale oppure cerca una città.",
    )

    LocationResolutionState.Resolving -> LocationStatusPresentation(
        title = "Aggiornamento posizione",
        message = "Sto cercando la posizione corrente.",
    )

    is LocationResolutionState.Ready -> {
        val isDevice = location.source is LocationSource.Device
        LocationStatusPresentation(
            title = if (isDevice) "Posizione attuale" else "Città selezionata",
            message = if (isDevice) {
                "Posizione pronta per orari e Qibla."
            } else {
                "Questa città verrà usata per i calcoli."
            },
            locationName = location.displayName,
            zoneId = location.zoneId.id,
        )
    }

    is LocationResolutionState.PermissionDenied -> LocationStatusPresentation(
        title = "Permesso posizione non concesso",
        message = appendSaved(
            if (canRequestAgain) {
                "Puoi riprovare oppure scegliere una città."
            } else {
                "Abilita la posizione dalle impostazioni dell’app oppure scegli una città."
            },
            cachedLocation,
        ),
        locationName = cachedLocation?.displayName,
        zoneId = cachedLocation?.zoneId?.id,
        showAppSettingsAction = !canRequestAgain,
    )

    is LocationResolutionState.LocationServicesDisabled -> LocationStatusPresentation(
        title = "Servizi di localizzazione disattivati",
        message = appendSaved(
            "Attiva la Posizione Android oppure scegli una città.",
            cachedLocation,
        ),
        locationName = cachedLocation?.displayName,
        zoneId = cachedLocation?.zoneId?.id,
        showLocationSettingsAction = true,
    )

    is LocationResolutionState.Unavailable -> LocationStatusPresentation(
        title = failureTitle(reason),
        message = appendSaved(failureMessage(reason), cachedLocation),
        locationName = cachedLocation?.displayName,
        zoneId = cachedLocation?.zoneId?.id,
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
        "Nessuna posizione corrente è arrivata entro 30 secondi. Puoi riprovare o scegliere una città manuale."
    LocationFailure.NO_PROVIDER ->
        "Il servizio di posizione non ha restituito un fix utilizzabile. Puoi riprovare o scegliere una città."
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

private fun appendSaved(base: String, cachedLocation: SelectedLocation?): String =
    if (cachedLocation == null) {
        base
    } else {
        "$base Ultima posizione salvata: ${cachedLocation.displayName}."
    }

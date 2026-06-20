package com.humblesolutions.humblecontacts.ui.profile

sealed interface CompleteProfileEvent {

    data object NavigateHome : CompleteProfileEvent

}
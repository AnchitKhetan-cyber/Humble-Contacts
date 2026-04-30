package com.humblesolutions.humblecontacts.ui.auth

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CountryCodeDropdown(
    selectedCountry: CountryCode,
    onCountrySelected: (CountryCode) -> Unit
) {

    var expanded by remember {
        mutableStateOf(false)
    }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = {
            expanded = !expanded
        }
    ) {

        OutlinedTextField(
            value = "${selectedCountry.flag} ${selectedCountry.dialCode}",
            onValueChange = {},
            readOnly = true,
            modifier = Modifier
                .menuAnchor()
                .width(120.dp),
            trailingIcon = {
                ExposedDropdownMenuDefaults.TrailingIcon(
                    expanded = expanded
                )
            }
        )

        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = {
                expanded = false
            }
        ) {

            countryCodes.forEach { country ->

                DropdownMenuItem(
                    text = {
                        Text(
                            "${country.flag} ${country.countryName} (${country.dialCode})"
                        )
                    },
                    onClick = {
                        onCountrySelected(country)
                        expanded = false
                    }
                )

            }
        }
    }
}
package com.humblesolutions.humblecontacts.ui.contacts

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Adds [raw] to [existing], returning a new list. Blank input is ignored and
 * duplicates are collapsed case-insensitively (so "Investor" and "investor"
 * count as the same tag); the first-seen spelling and order are preserved.
 * Ticket #16.
 */
fun addTag(existing: List<String>, raw: String): List<String> {
    val trimmed = raw.trim()
    if (trimmed.isEmpty()) return existing
    if (existing.any { it.equals(trimmed, ignoreCase = true) }) return existing
    return existing + trimmed
}

/** A single removable tag chip, matching the read-only chip style used elsewhere. */
@Composable
private fun RemovableTagChip(tag: String, onRemove: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(6.dp),
        color = MaterialTheme.colorScheme.primaryContainer
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(start = 10.dp, end = 2.dp, top = 2.dp, bottom = 2.dp)
        ) {
            Text(
                tag,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.primary
            )
            IconButton(onClick = onRemove, modifier = Modifier.size(24.dp)) {
                Icon(
                    Icons.Outlined.Close,
                    contentDescription = "Remove tag $tag",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

/**
 * Chip-style tag editor: a text field to add tags plus a wrapping row of
 * removable chips. Stateless over the tag list — the caller owns [tags] and
 * decides what persistence [onTagsChange] triggers (in-memory on Add, a
 * Firestore write on the detail screen). Ticket #16.
 */
@Composable
fun TagEditor(
    tags: List<String>,
    onTagsChange: (List<String>) -> Unit,
    modifier: Modifier = Modifier
) {
    var input by remember { mutableStateOf("") }

    val commit = {
        val updated = addTag(tags, input)
        if (updated !== tags) onTagsChange(updated)
        input = ""
    }

    androidx.compose.foundation.layout.Column(modifier = modifier.fillMaxWidth()) {
        OutlinedTextField(
            value = input,
            onValueChange = { input = it },
            singleLine = true,
            placeholder = { Text("Add a tag") },
            trailingIcon = {
                IconButton(onClick = commit, enabled = input.isNotBlank()) {
                    Icon(Icons.Outlined.Add, contentDescription = "Add tag")
                }
            },
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { commit() }),
            modifier = Modifier.fillMaxWidth()
        )

        if (tags.isNotEmpty()) {
            Spacer(Modifier.height(10.dp))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                tags.forEach { tag ->
                    RemovableTagChip(tag = tag, onRemove = { onTagsChange(tags - tag) })
                }
            }
        }
    }
}

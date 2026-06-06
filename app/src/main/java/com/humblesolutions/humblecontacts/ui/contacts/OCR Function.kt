package com.humblesolutions.humblecontacts.ui.contacts

import android.content.Context
import android.net.Uri
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions

fun processImage(
    context: Context,
    imageUri: Uri,
    onResult: (String) -> Unit
) {

    val image =
        InputImage.fromFilePath(
            context,
            imageUri
        )

    val recognizer =
        TextRecognition.getClient(
            TextRecognizerOptions.DEFAULT_OPTIONS
        )

    recognizer.process(image)
        .addOnSuccessListener {
            onResult(it.text)
        }
        .addOnFailureListener {
            onResult("")
        }
}
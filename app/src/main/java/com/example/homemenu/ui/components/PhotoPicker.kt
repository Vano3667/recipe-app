package com.example.homemenu.ui.components

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import coil.compose.AsyncImage
import java.io.File

/** Recipe/step photo or a soft placeholder. */
@Composable
fun RecipePhoto(file: File?, modifier: Modifier = Modifier, contentDescription: String? = null) {
    Box(
        modifier.clip(MaterialTheme.shapes.large).background(MaterialTheme.colorScheme.secondaryContainer),
        contentAlignment = Alignment.Center,
    ) {
        if (file != null) {
            AsyncImage(model = file, contentDescription = contentDescription, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
        } else {
            Icon(Icons.Filled.Restaurant, contentDescription = null, tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f), modifier = Modifier.size(40.dp))
        }
    }
}

/**
 * Gallery (Photo Picker — no storage permission needed) + Camera (TakePicture with runtime CAMERA permission).
 * [createCameraUri] must return a FileProvider content:// URI.
 *
 * TODO(фото): сейчас нигде не используется — добавление фото временно отключено. Подключается в
 *  RecipeEditRoute через параметр photoPicker у RecipeEditContent (см. TODO(фото) в RecipeEditScreen.kt).
 *  Камере нужно разрешение CAMERA в AndroidManifest.xml (сейчас закомментировано).
 */
@Composable
fun PhotoPickerField(
    photo: File?,
    onPicked: (Uri) -> Unit,
    onRemove: () -> Unit,
    createCameraUri: () -> Uri,
    modifier: Modifier = Modifier,
    tagPrefix: String = "photo",
) {
    val context = LocalContext.current
    var pendingCameraUri by rememberSaveable { mutableStateOf<Uri?>(null) }
    var showRationale by remember { mutableStateOf(false) }

    val gallery = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri -> uri?.let(onPicked) }
    val camera = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { ok ->
        val uri = pendingCameraUri
        if (ok && uri != null) onPicked(uri)
        pendingCameraUri = null
    }
    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) {
            val uri = createCameraUri()
            pendingCameraUri = uri
            camera.launch(uri)
        } else showRationale = true
    }

    Column(modifier) {
        RecipePhoto(photo, Modifier.fillMaxWidth().aspectRatio(16f / 9f), "Фото")
        Spacer(Modifier.size(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            OutlinedButton(
                onClick = { gallery.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
                modifier = Modifier.testTag("${tagPrefix}_gallery"),
            ) {
                Icon(Icons.Filled.Image, null); Spacer(Modifier.width(6.dp)); Text("Галерея")
            }
            OutlinedButton(
                onClick = {
                    val granted = ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
                    if (granted) {
                        val uri = createCameraUri()
                        pendingCameraUri = uri
                        camera.launch(uri)
                    } else permission.launch(Manifest.permission.CAMERA)
                },
                modifier = Modifier.testTag("${tagPrefix}_camera"),
            ) {
                Icon(Icons.Filled.CameraAlt, null); Spacer(Modifier.width(6.dp)); Text("Камера")
            }
            if (photo != null) {
                TextButton(onClick = onRemove) { Icon(Icons.Filled.Close, null); Text("Убрать") }
            }
        }
    }

    if (showRationale) {
        AlertDialog(
            onDismissRequest = { showRationale = false },
            confirmButton = { TextButton(onClick = { showRationale = false }) { Text("Понятно") } },
            title = { Text("Нужен доступ к камере") },
            text = { Text("Чтобы сфотографировать блюдо, разрешите доступ к камере в настройках. Можно также выбрать фото из галереи.") },
        )
    }
}

package com.baverelci.accessgrab.ui.components

import android.app.Activity
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.speech.RecognizerIntent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.baverelci.accessgrab.accessibility.AccessibilityHelper

@Composable
fun UrlInputCard(
    urlInput: String,
    onUrlChanged: (String) -> Unit,
    onClearInput: () -> Unit,
    onFetchMedia: () -> Unit,
    onPasteAndFetch: ((String) -> Unit)? = null,
    isLoading: Boolean,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val view = LocalView.current
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current

    fun hideKeyboard() {
        focusManager.clearFocus()
        keyboardController?.hide()
    }

    fun readClipboardText(): String {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
        return clipboard?.primaryClip?.getItemAt(0)?.text?.toString()?.trim() ?: ""
    }

    // Sesle Arama (Mikrofon) Başlatıcısı
    val speechLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val spokenList = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
            val spokenText = spokenList?.firstOrNull()?.trim() ?: ""
            if (spokenText.isNotBlank()) {
                hideKeyboard()
                if (onPasteAndFetch != null) {
                    onPasteAndFetch(spokenText)
                } else {
                    onUrlChanged(spokenText)
                    onFetchMedia()
                }
            }
        }
    }

    fun startVoiceSearch() {
        try {
            hideKeyboard()
            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, "tr-TR")
                putExtra(RecognizerIntent.EXTRA_PROMPT, "Aramak istediğiniz şarkı veya video adını söyleyin…")
            }
            speechLauncher.launch(intent)
        } catch (_: Exception) {
            AccessibilityHelper.announce(view, "Cihazınızda sesli arama servisi bulunamadı.")
        }
    }

    Column(modifier = modifier.fillMaxWidth()) {
        OutlinedTextField(
            value = urlInput,
            onValueChange = onUrlChanged,
            label = { Text("Bağlantı (URL) veya Arama Kelimesi") },
            placeholder = { Text("Şarkı, video adı yazın veya sosyal medya linki yapıştırın") },
            singleLine = false,
            maxLines = 3,
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Text,
                imeAction = ImeAction.Search
            ),
            keyboardActions = KeyboardActions(
                onSearch = {
                    hideKeyboard()
                    onFetchMedia()
                },
                onDone = {
                    hideKeyboard()
                    onFetchMedia()
                }
            ),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                focusedTextColor = MaterialTheme.colorScheme.onSurface,
                unfocusedTextColor = MaterialTheme.colorScheme.onSurface
            ),
            trailingIcon = {
                // Görsel ikonlar (Temizle, Sesle Ara, Yapıştır) gören kullanıcılar için kutunun sağında korunur;
                // TalkBack kullanıcıları bu işlemlere doğrudan kutunun Eylemler menüsünden erişir.
                Row(modifier = Modifier.clearAndSetSemantics { }) {
                    if (urlInput.isNotEmpty()) {
                        IconButton(onClick = onClearInput) {
                            Icon(
                                imageVector = Icons.Default.Clear,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                    IconButton(onClick = { startVoiceSearch() }) {
                        Icon(
                            imageVector = Icons.Default.Mic,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.secondary
                        )
                    }
                    IconButton(
                        onClick = {
                            val clip = readClipboardText()
                            if (clip.isNotBlank()) {
                                hideKeyboard()
                                onUrlChanged(clip)
                            }
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.ContentPaste,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .semantics {
                    this.contentDescription = "Medya arama veya bağlantı giriş kutusu. ${if (urlInput.isEmpty()) "Şu an boş. Şarkı, video adı yazabilir veya link yapıştırabilirsiniz." else "İçerik: $urlInput"}"
                    val actions = mutableListOf<CustomAccessibilityAction>()
                    actions.add(
                        CustomAccessibilityAction("Sesle Ara (Mikrofon)") {
                            startVoiceSearch()
                            true
                        }
                    )
                    actions.add(
                        CustomAccessibilityAction("Panodan Yapıştır ve Hemen İncele") {
                            val clip = readClipboardText()
                            if (clip.isNotBlank()) {
                                hideKeyboard()
                                if (onPasteAndFetch != null) {
                                    onPasteAndFetch(clip)
                                } else {
                                    onUrlChanged(clip)
                                    onFetchMedia()
                                }
                            }
                            true
                        }
                    )
                    actions.add(
                        CustomAccessibilityAction("Panodan Yapıştır") {
                            val clip = readClipboardText()
                            if (clip.isNotBlank()) {
                                hideKeyboard()
                                onUrlChanged(clip)
                            }
                            true
                        }
                    )
                    if (urlInput.isNotBlank()) {
                        actions.add(
                            CustomAccessibilityAction("Ara veya Medyayı İncele") {
                                hideKeyboard()
                                onFetchMedia()
                                true
                            }
                        )
                        actions.add(
                            CustomAccessibilityAction("Metni Temizle") {
                                onClearInput()
                                true
                            }
                        )
                    }
                    this.customActions = actions
                }
        )

        Spacer(modifier = Modifier.height(14.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(10.dp)
        ) {
            AccessibleButton(
                text = "Ara / İncele",
                contentDesc = "Girilen kelimeyi ara veya bağlantıdaki medyayı incele",
                icon = Icons.Default.Search,
                modifier = Modifier.weight(1f),
                onClick = {
                    hideKeyboard()
                    onFetchMedia()
                },
                enabled = !isLoading
            )

            AccessibleButton(
                text = "Sesle Ara",
                contentDesc = "Mikrofonu aç ve konuşarak şarkı veya video ara",
                icon = Icons.Default.Mic,
                containerColor = MaterialTheme.colorScheme.secondary,
                contentColor = MaterialTheme.colorScheme.onSecondary,
                modifier = Modifier.weight(0.75f),
                onClick = { startVoiceSearch() },
                enabled = !isLoading
            )
        }
    }
}

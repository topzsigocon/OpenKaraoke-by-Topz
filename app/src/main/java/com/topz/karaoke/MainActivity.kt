package com.topz.karaoke

import android.Manifest
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import com.topz.karaoke.ui.KaraokeRoot
import com.topz.karaoke.ui.KaraokeTheme
import com.topz.karaoke.ui.KaraokeViewModel

class MainActivity : ComponentActivity() {

    private val vm: KaraokeViewModel by viewModels()

    private val legacyPermissions = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { vm.onResume() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(Color.TRANSPARENT)
        )
        setContent {
            KaraokeTheme {
                KaraokeRoot(vm = vm, onRequestAccess = ::requestAccess)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // Pagbalik galing sa Settings, i-check ulit ang permission.
        vm.onResume()
    }

    override fun onStop() {
        super.onStop()
        vm.onAppStopped()
    }

    private fun requestAccess() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            startActivity(
                Intent(
                    Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION,
                    Uri.parse("package:$packageName")
                )
            )
        } else {
            legacyPermissions.launch(
                arrayOf(
                    Manifest.permission.READ_EXTERNAL_STORAGE,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
                )
            )
        }
    }
}
